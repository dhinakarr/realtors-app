package com.realtors.alerts.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

public class DeviceTokenRowMapper implements RowMapper<String> {

    @Override
    public String mapRow(ResultSet rs, int rowNum) throws SQLException {
        return rs.getString("device_token");
    }
}
