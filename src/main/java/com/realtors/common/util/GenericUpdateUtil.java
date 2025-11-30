package com.realtors.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class GenericUpdateUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final Logger logger = LoggerFactory.getLogger(GenericUpdateUtil.class);
    private static final Set<String> JSONB_COLUMNS = Set.of("meta");
    private static final Set<String> UUID_COLUMNS = Set.of("roleId", "managerId", "updatedBy", "createdBy");
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
     * ✅ Patch with file support
     * updates Map can contain:
     * - primitives / strings
     * - Map / List (JSON)
     * - MultipartFile (will store as bytea or you can customize to path)
     */
    public static <T> T patchGenericWithFileSupport(
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
        return executeUpdateWithFiles(tableName, updates, jdbcTemplate, dtoClass, idColumn, idValue);
    }

    /**
     * ✅ Internal method for executing update
     */
    private static <T> T executeUpdateWithFiles(
            String tableName,
            Map<String, Object> updates,
            JdbcTemplate jdbcTemplate,
            Class<T> dtoClass,
            String idColumn,
            Object idValue
    ) {
        // Add audit fields
        UUID currentUser = AppUtil.getCurrentUserId();
        if (currentUser != null) updates.put("updated_by", currentUser);
        updates.put("updated_at", OffsetDateTime.now());
        StringBuilder sql = new StringBuilder("UPDATE ").append(tableName).append(" SET ");
        List<Object> params = new ArrayList<>();
        boolean first = true;
        for (Map.Entry<String, Object> entry : updates.entrySet()) {
        	String key = entry.getKey();
            String column = camelToSnake(entry.getKey());
            Object value = entry.getValue();
            boolean isJsonbType = JSONB_COLUMNS.contains(key);
            boolean requiresUuidCast = UUID_COLUMNS.contains(key);
            
            if (value instanceof MultipartFile file) {
				try {
					value = file.getBytes();
				} catch (Exception e) {
					throw new RuntimeException("Failed to read file for field: " + column, e);
				}
			} else if (value instanceof Map || value instanceof List || value instanceof LinkedHashMap) {
				
				org.postgresql.util.PGobject jsonbObject = new org.postgresql.util.PGobject();
				jsonbObject.setType("jsonb");
				try {
					String jsonString = value != null ? new ObjectMapper().writeValueAsString(value) : "{}";
					jsonbObject.setValue(jsonString);
				} catch (JsonProcessingException e) {
					logger.info("@GenericInsertUtil.insertGenericWithFileSupport Json	rocessingException: " + e);
					throw new RuntimeException(e);
				} catch (SQLException sqle) {
					logger.info("@GenericInsertUtil.insertGenericWithFileSupport JsonProcessingException: " + sqle);
					throw new RuntimeException(sqle);
				}
				value = jsonbObject;
			}
            if (requiresUuidCast && value instanceof java.util.UUID) {
                requiresUuidCast = true; 
            }
            if (!first) {
                sql.append(", ");
            }
            sql.append(column).append(" = ?");
            logger.info("@GenericInsertUtil.insertGenericWithFileSupport isJsonbType: " + isJsonbType);
            if (isJsonbType) {
                sql.append("::jsonb"); // Add the explicit cast ONLY for JSON/Map/List
                logger.info("@GenericInsertUtil.insertGenericWithFileSupport sql.toString(): " + sql.toString());
            } else if (requiresUuidCast) {
                sql.append("::uuid");
            }
            params.add(value);
            first = false;
        }
        if (first) {
            logger.warn("No fields to update for ID: " + idValue);
            return findById(tableName, idColumn, idValue, jdbcTemplate, dtoClass);
        }

//        sql.setLength(sql.length() - 2); // remove trailing comma
        buildWhereClause(sql, params, idColumn, idValue);
        logger.info("@GenericUpdateUtil.executeUpdateWithFiles  sql: "+sql);
        // Execute
        jdbcTemplate.update(sql.toString(), params.toArray());

        // Return updated record
        return findById(tableName, idColumn, idValue, jdbcTemplate, dtoClass);
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

            if (isJson(value)) {
                sql.append(column).append(" = ?::jsonb, ");
                try {
                	params.add(value instanceof String ? value : objectMapper.writeValueAsString(value));
                } catch (JsonProcessingException ex) {
                	throw new RuntimeException("Error in Json data processing: " + column, ex);
                }
            } else {
                sql.append(column).append(" = ?, ");
                params.add(convertIfUuid(value));
            }
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

    public static Object convertIfUuid(Object value) {
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

    public static String camelToSnake(String name) {
        return name.replaceAll("([a-z])([A-Z]+)", "$1_$2").toLowerCase();
    }
    
    private static boolean isJson(Object value) {
        return (value instanceof Map) || (value instanceof List) ||
               (value instanceof String s && s.trim().startsWith("{"));
    }
}
