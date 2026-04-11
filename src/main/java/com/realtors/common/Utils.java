package com.realtors.common;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Utils {
	
	// this helps to type cast the number fields from the incoming Map to avoid type cast exception while DB operation
	public static Map<String, Object> castNumberFields(Map<String, Object> allFields, Set<String>  integerFields) {
	    for (Map.Entry<String, Object> entry : allFields.entrySet()) {
	        String key = entry.getKey();
	        Object value = entry.getValue();

	        if (integerFields.contains(key) && value instanceof String) {
	            try {
	                Integer intValue = Integer.parseInt((String) value);
	                allFields.put(key, intValue);
	            } catch (NumberFormatException e) {
	                throw new IllegalArgumentException("Invalid number format for field: " + key);
	            }
	        }
	    }
	    return allFields;
	}
	
	public static Map<String, Object> castDateFields(Map<String, Object> fields, Set<String> dateFields) {
	    Map<String, Object> updated = new HashMap<>(fields);

	    for (String key : dateFields) {
	        Object value = updated.get(key);

	        if (value != null && value instanceof String str && !str.isBlank()) {
	            try {
	                updated.put(key, java.sql.Date.valueOf(str)); // ✅ convert to SQL Date
	            } catch (IllegalArgumentException e) {
	                throw new RuntimeException("Invalid date format for field: " + key + ". Expected yyyy-MM-dd");
	            }
	        }
	    }

	    return updated;
	}

}
