package com.realtors.common.util;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class GenericUpdatesUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static <T> T saveOrPatchGeneric(
            String tableName,
            Object data, // can be DTO or Map<String, Object>
            JdbcTemplate jdbcTemplate,
            Class<T> dtoClass,
            String idColumn,
            Object idValue,
            boolean isFullUpdate
    ) {
        // Convert DTO -> map if needed
        Map<String, Object> values = (data instanceof Map)
                ? new LinkedHashMap<>((Map<String, Object>) data)
                : GenericInsertUtil.toColumnMap(data, idColumn);

        // ✅ Add audit fields
        UUID currentUser = AppUtil.getCurrentUserId();
        if (currentUser != null) values.put("updated_by", currentUser);
        values.put("updated_at", OffsetDateTime.now());

        // ✅ Build SQL dynamically
        StringBuilder sql = new StringBuilder("UPDATE ").append(tableName).append(" SET ");
        List<Object> params = new ArrayList<>();

        for (Map.Entry<String, Object> entry : values.entrySet()) {
            String column = camelToSnake(entry.getKey());
            Object value = entry.getValue();

            // Serialize JSON columns safely
            if (value instanceof Map || value instanceof List) {
                try {
                    value = objectMapper.writeValueAsString(value);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException("Failed to serialize JSON field: " + column, e);
                }
            }

            sql.append(column).append(" = ?, ");
            params.add(convertIfUuid(value));
        }

        sql.setLength(sql.length() - 2); // Remove trailing comma

        // ✅ Handle WHERE clause (single or composite UUID keys)
        appendWhereClause(sql, params, idColumn, idValue);

        // ✅ Execute update
        jdbcTemplate.update(sql.toString(), params.toArray());

        // ✅ Return updated record
        return findById(tableName, idColumn, idValue, jdbcTemplate, dtoClass);
    }

    // Helper to handle WHERE clause
    private static void appendWhereClause(StringBuilder sql, List<Object> params, String idColumn, Object idValue) {
        List<String> idColumns = Arrays.stream(idColumn.split(","))
                .map(String::trim)
                .collect(Collectors.toList());

        sql.append(" WHERE ");

        if (idColumns.size() == 1) {
            sql.append(idColumns.get(0)).append(" = ?::uuid");
            params.add(convertIfUuid(idValue));
        } else {
            List<String> conditions = new ArrayList<>();
            if (idValue instanceof Map<?, ?> idMap) {
                for (String col : idColumns) {
                    conditions.add(col + " = ?::uuid");
                    params.add(convertIfUuid(idMap.get(col)));
                }
            } else if (idValue instanceof List<?> idList) {
                for (int i = 0; i < idColumns.size(); i++) {
                    conditions.add(idColumns.get(i) + " = ?::uuid");
                    params.add(convertIfUuid(idList.get(i)));
                }
            } else {
                throw new IllegalArgumentException("Composite key requires Map or List for idValue");
            }
            sql.append(String.join(" AND ", conditions));
        }
    }

    // ✅ UUID converter (strict)
    private static Object convertIfUuid(Object value) {
        if (value == null) return null;
        if (value instanceof UUID) return value;
        if (value instanceof String str && str.matches("^[0-9a-fA-F-]{36}$")) {
            return UUID.fromString(str);
        }
        return value;
    }

    // ✅ Utility (shared by update/patch)
    private static String camelToSnake(String input) {
        return input.replaceAll("([a-z])([A-Z]+)", "$1_$2").toLowerCase();
    }

    // ✅ Find updated record
    private static <T> T findById(
            String tableName,
            String idColumn,
            Object idValue,
            JdbcTemplate jdbcTemplate,
            Class<T> dtoClass
    ) {
    	StringBuilder sql = new StringBuilder("SELECT * FROM ").append(tableName);
        List<Object> params = new ArrayList<>();

        buildWhereClause(sql, params, idColumn, idValue);
        return jdbcTemplate.queryForObject(sql.toString(), new JsonAwareRowMapper<>(dtoClass), params.toArray());
    }
    
    /**
     * ✅ Common WHERE clause builder supporting composite UUID keys
     */
    private static void buildWhereClause(
            StringBuilder sql,
            List<Object> params,
            String idColumn,
            Object idValue
    ) {
        List<String> idColumns = Arrays.stream(idColumn.split(","))
                .map(String::trim)
                .collect(Collectors.toList());

        sql.append(" WHERE ");

        if (idColumns.size() == 1) {
            sql.append(idColumns.get(0)).append(" = ?::uuid");
            params.add(convertIfUuid(idValue));
        } else {
            List<String> conditions = new ArrayList<>();
            List<Object> idValues = new ArrayList<>();

            if (idValue instanceof Map<?, ?> idMap) {
                for (String col : idColumns) {
                    conditions.add(col + " = ?::uuid");
                    idValues.add(convertIfUuid(idMap.get(col)));
                }
            } else if (idValue instanceof List<?> idList) {
                for (int i = 0; i < idColumns.size(); i++) {
                    conditions.add(idColumns.get(i) + " = ?::uuid");
                    idValues.add(convertIfUuid(idList.get(i)));
                }
            } else {
                throw new IllegalArgumentException("Composite key requires Map or List for idValue");
            }

            sql.append(String.join(" AND ", conditions));
            params.addAll(idValues);
        }
    }
    
}

