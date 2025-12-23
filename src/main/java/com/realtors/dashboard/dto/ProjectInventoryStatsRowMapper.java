package com.realtors.dashboard.dto;

import org.springframework.jdbc.core.RowMapper;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ProjectInventoryStatsRowMapper
        implements RowMapper<ProjectInventoryStatsDTO> {

    @Override
    public ProjectInventoryStatsDTO mapRow(ResultSet rs, int rowNum)
            throws SQLException {

        ProjectInventoryStatsDTO dto = new ProjectInventoryStatsDTO();
        dto.setProjectId(rs.getObject("project_id", java.util.UUID.class));
        dto.setProjectName(rs.getString("project_name"));
        dto.setStatus(rs.getString("inventory_status"));
        dto.setCount(rs.getLong("count"));

        return dto;
    }
}

