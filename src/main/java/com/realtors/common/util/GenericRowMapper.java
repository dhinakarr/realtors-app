package com.realtors.common.util;

import org.springframework.jdbc.core.RowMapper;
import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

public class GenericRowMapper<T> implements RowMapper<T> {
    private final Class<T> type;

    public GenericRowMapper(Class<T> type) {
        this.type = type;	
    }

    @Override
    public T mapRow(ResultSet rs, int rowNum) throws SQLException {
        try {
            T instance = type.getDeclaredConstructor().newInstance();
            ResultSetMetaData meta = rs.getMetaData();
            
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                String column = meta.getColumnLabel(i); // Use label to support aliases
                Object val = rs.getObject(i);
                String fieldName = snakeToCamel(column);

                try {
                    Field field = type.getDeclaredField(fieldName);
                    field.setAccessible(true);

                    if (val != null) {
                        Class<?> fieldType = field.getType();
                        if (fieldType == OffsetDateTime.class && val instanceof java.sql.Timestamp ts) {
                            field.set(instance, ts.toInstant().atOffset(ZoneOffset.UTC));
                        } else if (fieldType == UUID.class && val instanceof java.util.UUID) {
                            field.set(instance, val);
                        } else {
                            field.set(instance, val);
                        }
                    }
                } catch (NoSuchFieldException ignored) {
                    // ignore unmapped columns
                }
            }
            return instance;
        } catch (Exception e) {
            throw new RuntimeException("Error mapping row to " + type.getSimpleName(), e);
        }
    }

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
    /*
    private String camelToSnake(String name) {
        return name.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }
    */
}

