package com.realtors.common.util;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

import org.postgresql.util.PGobject;
import org.springframework.jdbc.core.BeanPropertyRowMapper;

public class JsonAwareRowMapper<T> extends BeanPropertyRowMapper<T> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public JsonAwareRowMapper(Class<T> mappedClass) {
        super(mappedClass);
    }

    @Override
    protected Object getColumnValue(java.sql.ResultSet rs, int index, java.beans.PropertyDescriptor pd)
            throws java.sql.SQLException {

        Object value = rs.getObject(index);
        if (value instanceof PGobject pgObject && "jsonb".equals(pgObject.getType())) {
            try {
                String json = pgObject.getValue();
                if (json != null) {
                    return objectMapper.readValue(json, Map.class);
                }
                return null;
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse JSONB column " + pd.getName(), e);
            }
        }
        return value;
    }
}
