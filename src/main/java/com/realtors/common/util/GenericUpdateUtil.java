package com.realtors.common.util;

import org.springframework.jdbc.core.JdbcTemplate;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class GenericUpdateUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * ✅ Full update (DTO → Map)
     */
    public static <T> T updateGeneric(
            String tableName,
            T dto,
            JdbcTemplate jdbcTemplate,
            Class<T> dtoClass,
            String idColumn,
            Object idValue
    ) {
        Map<String, Object> values = GenericInsertUtil.toColumnMap(dto, idColumn);
        return executeUpdate(tableName, values, jdbcTemplate, dtoClass, idColumn, idValue);
    }

    /**
     * ✅ Partial update (PATCH → Map)
     */
    public static <T> T patchGeneric(
            String tableName,
            Map<String, Object> updates,
            JdbcTemplate jdbcTemplate,
            Class<T> dtoClass,
            String idColumn,
            Object idValue
    ) {
        if (updates == null || updates.isEmpty()) {
            throw new IllegalArgumentException("No fields to update");
        }
        return executeUpdate(tableName, updates, jdbcTemplate, dtoClass, idColumn, idValue);
    }

    /**
     * ✅ Common internal method for both update/patch
     */
    private static <T> T executeUpdate(
            String tableName,
            Map<String, Object> updates,
            JdbcTemplate jdbcTemplate,
            Class<T> dtoClass,
            String idColumn,
            Object idValue
    ) {
        // ✅ Add audit fields
        UUID currentUser = AppUtil.getCurrentUserId();
        if (currentUser != null) updates.put("updated_by", currentUser);
        updates.put("updated_at", OffsetDateTime.now());

        StringBuilder sql = new StringBuilder("UPDATE ").append(tableName).append(" SET ");
        List<Object> params = new ArrayList<>();

        // ✅ Build SET clause
        for (Map.Entry<String, Object> entry : updates.entrySet()) {
            String column = camelToSnake(entry.getKey());
            Object value = entry.getValue();

            // Serialize JSON fields
            if (value instanceof Map || value instanceof List) {
                try {
                    value = objectMapper.writeValueAsString(value);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException("Failed to serialize field: " + column, e);
                }
            }

            sql.append(column).append(" = ?, ");
            params.add(convertIfUuid(value));
        }

        sql.setLength(sql.length() - 2); // remove trailing comma

        // ✅ Build WHERE clause
        buildWhereClause(sql, params, idColumn, idValue);

        // ✅ Execute update
        jdbcTemplate.update(sql.toString(), params.toArray());

        // ✅ Return updated record
        return findById(tableName, idColumn, idValue, jdbcTemplate, dtoClass);
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

    /**
     * ✅ Safe findById with UUID & composite key handling
     */
    public static <T> T findById(
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

    private static Object convertIfUuid(Object value) {
        if (value == null) return null;
        if (value instanceof UUID) return value;
        if (value instanceof String s) {
            try {
                return UUID.fromString(s);
            } catch (IllegalArgumentException ignored) {
                // not a UUID string
            }
        }
        return value;
    }

    private static String camelToSnake(String name) {
        return name.replaceAll("([a-z])([A-Z]+)", "$1_$2").toLowerCase();
    }
}
