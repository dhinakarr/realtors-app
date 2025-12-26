package com.realtors.dashboard.repository;

import org.springframework.jdbc.core.RowMapper;

import com.realtors.dashboard.dto.InventoryDetailDTO;

import java.sql.ResultSet;
import java.sql.SQLException;

public class InventoryStatsRowMapper implements RowMapper<InventoryDetailDTO> {

    @Override
    public InventoryDetailDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
        InventoryDetailDTO dto = new InventoryDetailDTO();
        dto.setStatus(rs.getString("status"));
        dto.setCount(rs.getLong("count"));
        return dto;
    }
}
