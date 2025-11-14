package com.realtors.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.realtors.admin.service.AppUserService;

import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.*;

public class GenericInsertUtil {

	private GenericInsertUtil() {
	}
	
	private static final ObjectMapper objectMapper = new ObjectMapper();
	private static final Logger logger = LoggerFactory.getLogger(AppUserService.class);
	/**
     * Inserts a DTO into the given table and returns the fully populated DTO
     * after insertion (with generated ID, timestamps, audit fields).
     *
     * @param tableName table to insert into
     * @param dto       DTO object to insert
     * @param jdbcTemplate JdbcTemplate instance
     * @param dtoClass  DTO class
     * @param idColumn  primary key column name
     * @param <T>       type of DTO
     * @return inserted DTO with all fields populated
     */
	public static <T> T insertGeneric(
	        String tableName,
	        T dto,
	        JdbcTemplate jdbcTemplate,
	        Class<T> dtoClass,
	        String idColumn
	) {
	    // 1️⃣ Convert DTO to column map
	    LinkedHashMap<String,Object> values = toColumnMap(dto, idColumn);

	    // 2️⃣ Add audit/status fields
	    Object status = values.get("status");
	    if (!(status instanceof String s) || s.isBlank()) {
	        values.put("status", "ACTIVE");
	    } else {
	        values.put("status", s.toUpperCase());
	    }
	    values.putIfAbsent("created_at", OffsetDateTime.now());
	    values.putIfAbsent("updated_at", OffsetDateTime.now());
	    
	    UUID currentUser = AppUtil.getCurrentUserId();
//	    if (currentUser != null) {
	        values.putIfAbsent("created_by", currentUser);
	        values.putIfAbsent("updated_by", currentUser);
//	    }
	    // 3️⃣ Build INSERT SQL dynamically
	    StringBuilder sql = new StringBuilder("INSERT INTO " + tableName + " (");
	    StringBuilder placeholders = new StringBuilder();
	    List<Object> params = new ArrayList<>();

	    for (String col : values.keySet()) {
	        sql.append(col).append(", ");
	        placeholders.append("?, ");
	        params.add(values.get(col));
	    }

	    // Remove last comma
	    sql.setLength(sql.length() - 2);
	    placeholders.setLength(placeholders.length() - 2);

	    sql.append(") VALUES (").append(placeholders).append(") RETURNING *");

	    // 5️⃣ Execute query
	    return jdbcTemplate.queryForObject(sql.toString(), new JsonAwareRowMapper<>(dtoClass), params.toArray());
	}
	
	/**
	 * Converts a DTO object into a Map of column_name -> value, automatically
	 * converting camelCase field names to snake_case.
	 * @param dto      DTO object
     * @param idColumn primary key column name
     * @param <T>      type of DTO
     * @return LinkedHashMap of column names (snake_case) to values
	 */
	public static LinkedHashMap<String,Object> toColumnMap(Object dto, String idColumn) {
        LinkedHashMap<String,Object> map = new LinkedHashMap<>();
        for (Field field : dto.getClass().getDeclaredFields()) {
            int mod = field.getModifiers();
            if (java.lang.reflect.Modifier.isStatic(mod) || java.lang.reflect.Modifier.isTransient(mod)) continue;

            field.setAccessible(true);
            try {
                Object value = field.get(dto);
                if (value != null && !field.getName().equalsIgnoreCase(idColumn)) {
                	// Serialize Maps/Lists to JSON string for JSONB columns
                    if (value instanceof Map || value instanceof List) {
                    	try {
                    		value = objectMapper.writeValueAsString(value);
                    	} catch (JsonProcessingException jpe) {
                    		throw new RuntimeException("Failed to serialize field: " + jpe);
                    	}
                    }
                    map.put(camelToSnake(field.getName()), value);
                }
            } catch (IllegalAccessException ignored) {}
        }
        return map;
    }

	/**
     * Converts camelCase to snake_case
     */
	public static String camelToSnake(String name) {
		StringBuilder sb = new StringBuilder();
        for (char c : name.toCharArray()) {
            if (Character.isUpperCase(c)) {
                sb.append("_").append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }	
        }
        return sb.toString();
    }

	/*
	private static String toSnakeCase(String camelCase) {
		return camelCase.replaceAll("([a-z])([A-Z]+)", "$1_$2").toLowerCase();
	}
	*/
	
	public static String snakeToCamel(String name) {
        StringBuilder sb = new StringBuilder();
        boolean upper = false;
        for (char c : name.toCharArray()) {
            if (c == '_') upper = true;
            else sb.append(upper ? Character.toUpperCase(c) : c);
            upper = false;
        }
        return sb.toString();
    }
}
