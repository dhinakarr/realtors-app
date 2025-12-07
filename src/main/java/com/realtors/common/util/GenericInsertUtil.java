package com.realtors.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.*;

public class GenericInsertUtil {

	private GenericInsertUtil() {
	}

	private static final ObjectMapper objectMapper = new ObjectMapper();
	private static final Logger logger = LoggerFactory.getLogger(GenericInsertUtil.class);

	/**
	 * Inserts a DTO into the given table and returns the fully populated DTO after
	 * insertion (with generated ID, timestamps, audit fields).
	 *
	 * @param tableName    table to insert into
	 * @param dto          DTO object to insert
	 * @param jdbcTemplate JdbcTemplate instance
	 * @param dtoClass     DTO class
	 * @param idColumn     primary key column name
	 * @param <T>          type of DTO
	 * @return inserted DTO with all fields populated
	 */
	public static <T> T insertGeneric(String tableName, T dto, JdbcTemplate jdbcTemplate, Class<T> dtoClass,
			String idColumn) {
		// 1️⃣ Convert DTO to column map
		LinkedHashMap<String, Object> values = toColumnMap(dto, idColumn);

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

	public static <T> T insertGenericWithFileSupport(String tableName, Map<String, Object> data,
			JdbcTemplate jdbcTemplate, Class<T> dtoClass, Set<String> excludeColumns) {
		// Add audit fields
		UUID currentUser = AppUtil.getCurrentUserId();
		if (currentUser != null)
			data.put("created_by", currentUser);
		data.put("created_at", OffsetDateTime.now());
		data.put("status", "ACTIVE");

		StringBuilder sql = new StringBuilder("INSERT INTO ").append(tableName).append(" (");
		StringBuilder placeholders = new StringBuilder("(");
		List<Object> params = new ArrayList<>();
		Set<String> addedColumns = new HashSet<>();

		for (Map.Entry<String, Object> entry : data.entrySet()) {
			String fieldName = entry.getKey();
			String col = GenericUpdateUtil.camelToSnake(fieldName);
			
			// SKIP auto-generated columns
		    if ("userId".equalsIgnoreCase(fieldName) || 
		        "user_id".equalsIgnoreCase(col) ||
		        "createdAt".equalsIgnoreCase(fieldName) ||
		        "updatedAt".equalsIgnoreCase(fieldName)) {
		        continue; // Let DB generate these
		    }
		    
		    if (excludeColumns != null && (excludeColumns.contains(fieldName) || excludeColumns.contains(col))) {
		        continue; // skip this field
		    }
		    
			if (addedColumns.contains(col)) {
				continue;
			}
			addedColumns.add(col);

			Object value = entry.getValue();
			if (value instanceof MultipartFile file) {
				try {
					value = file.getBytes();
				} catch (Exception e) {
					logger.info("@GenericInsertUtil.insertGenericWithFileSupport image read Exception: " + e);
					throw new RuntimeException("Failed to read file for field: " + col, e);
				}
			} else if (value instanceof Map || value instanceof List) {
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

			sql.append(col).append(",");
			placeholders.append("?,");
			params.add(GenericUpdateUtil.convertIfUuid(value));
		}

		sql.setLength(sql.length() - 1);
		placeholders.setLength(placeholders.length() - 1);
		sql.append(") VALUES ").append(placeholders).append(")");

		jdbcTemplate.update(sql.toString(), params.toArray());

		// Optionally return last inserted record if table has UUID PK
		return jdbcTemplate.queryForObject("SELECT * FROM " + tableName + " ORDER BY created_at DESC LIMIT 1",
				new JsonAwareRowMapper<>(dtoClass));
	}

	/**
	 * Converts a DTO object into a Map of column_name -> value, automatically
	 * converting camelCase field names to snake_case.
	 * 
	 * @param dto      DTO object
	 * @param idColumn primary key column name
	 * @param <T>      type of DTO
	 * @return LinkedHashMap of column names (snake_case) to values
	 */
	public static LinkedHashMap<String, Object> toColumnMap(Object dto, String idColumn) {
		LinkedHashMap<String, Object> map = new LinkedHashMap<>();
		for (Field field : dto.getClass().getDeclaredFields()) {
			int mod = field.getModifiers();
			if (java.lang.reflect.Modifier.isStatic(mod) || java.lang.reflect.Modifier.isTransient(mod))
				continue;

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
			} catch (IllegalAccessException ignored) {
			}
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
	 * private static String toSnakeCase(String camelCase) { return
	 * camelCase.replaceAll("([a-z])([A-Z]+)", "$1_$2").toLowerCase(); }
	 */

	public static String snakeToCamel(String name) {
		StringBuilder sb = new StringBuilder();
		boolean upper = false;
		for (char c : name.toCharArray()) {
			if (c == '_')
				upper = true;
			else
				sb.append(upper ? Character.toUpperCase(c) : c);
			upper = false;
		}
		return sb.toString();
	}
}
