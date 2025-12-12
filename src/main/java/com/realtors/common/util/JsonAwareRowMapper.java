package com.realtors.common.util;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.postgresql.util.PGobject;
import org.springframework.jdbc.core.BeanPropertyRowMapper;

public class JsonAwareRowMapper<T> extends BeanPropertyRowMapper<T> {

	private static final ObjectMapper objectMapper = new ObjectMapper();

	public JsonAwareRowMapper(Class<T> mappedClass) {
		super(mappedClass);
	}
	/*
	 * @Override public T mapRow(ResultSet rs, int rowNum) throws SQLException {
	 * ResultSetMetaData meta = rs.getMetaData(); Map<String, Object> rowMap = new
	 * LinkedHashMap<>();
	 * 
	 * for (int i = 1; i <= meta.getColumnCount(); i++) { String columnLabel =
	 * meta.getColumnLabel(i); // uses alias if present Object raw =
	 * rs.getObject(i);
	 * 
	 * // Handle JSONB (Postgres) → Map if (raw instanceof PGobject pg &&
	 * "jsonb".equals(pg.getType())) { String json = pg.getValue(); if (json !=
	 * null) { try { raw = objectMapper.readValue(json, Map.class); } catch
	 * (Exception e) { throw new SQLException("Failed to parse jsonb column " +
	 * columnLabel, e); } } else { raw = null; } }
	 * 
	 * // Normalize boolean-ish strings (rare): "true"/"false" or "t"/"f" if (raw
	 * instanceof String) { String s = ((String) raw).trim(); if
	 * ("true".equalsIgnoreCase(s) || "t".equalsIgnoreCase(s) || "1".equals(s)) {
	 * raw = Boolean.TRUE; } else if ("false".equalsIgnoreCase(s) ||
	 * "f".equalsIgnoreCase(s) || "0".equals(s)) { raw = Boolean.FALSE; } }
	 * 
	 * // Convert column label (snake_case) -> property name (camelCase) String
	 * propName = snakeToCamel(columnLabel);
	 * 
	 * rowMap.put(propName, raw); }
	 * 
	 * // Use Jackson to convert the flat map into your DTO class // This will do
	 * type coercion (booleans, numbers, UUIDs) based on DTO fields. try { return
	 * objectMapper.convertValue(rowMap, getMappedClass()); } catch
	 * (IllegalArgumentException ex) { // Fallback: let super attempt a default
	 * mapping // (rarely needed but safe) T instance = null; try { instance =
	 * super.mapRow(rs, rowNum); } catch (Exception e) { throw new
	 * SQLException("Failed to map row to " + getMappedClass(), e); } return
	 * instance; } }
	 * 
	 * private static String snakeToCamel(String input) { if (input == null) return
	 * null; // remove quotes if any and lower-case String s = input.replace("\"",
	 * "").toLowerCase(Locale.ROOT); StringBuilder sb = new StringBuilder(); boolean
	 * upperNext = false; for (char c : s.toCharArray()) { if (c == '_' || c == ' ')
	 * { upperNext = true; } else { if (upperNext) {
	 * sb.append(Character.toUpperCase(c)); upperNext = false; } else {
	 * sb.append(c); } } } return sb.toString(); }
	 */
	
	  @Override protected Object getColumnValue(java.sql.ResultSet rs, int index,
	  java.beans.PropertyDescriptor pd) throws java.sql.SQLException {
	  
	  Object value = rs.getObject(index); if (value instanceof PGobject pgObject &&
	  "jsonb".equals(pgObject.getType())) { try { String json =
	  pgObject.getValue(); if (json != null) { return objectMapper.readValue(json,
	  Map.class); } return null; } catch (Exception e) { throw new
	  RuntimeException("Failed to parse JSONB column " + pd.getName(), e); } }
	  return value; }
	 
}
