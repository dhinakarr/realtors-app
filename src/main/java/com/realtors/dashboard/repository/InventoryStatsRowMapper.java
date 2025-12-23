package com.realtors.dashboard.repository;

import org.springframework.jdbc.core.RowMapper;

import com.realtors.dashboard.dto.InventoryStatsDTO;

import java.sql.ResultSet;
import java.sql.SQLException;

public class InventoryStatsRowMapper implements RowMapper<InventoryStatsDTO> {

    @Override
    public InventoryStatsDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
        InventoryStatsDTO dto = new InventoryStatsDTO();
        dto.setStatus(rs.getString("status"));
        dto.setCount(rs.getLong("count"));
        return dto;
    }
}
