package com.realtors.sitevisit.mapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import org.springframework.jdbc.core.RowMapper;
import com.realtors.sitevisit.dto.ProjectMiniDto;

public class SiteProjectRowMapper implements RowMapper<ProjectMiniDto> {

    @Override
    public ProjectMiniDto mapRow(ResultSet rs, int rowNum) throws SQLException {

    	ProjectMiniDto dto = new ProjectMiniDto();
        dto.setProjectId(UUID.fromString(rs.getString("project_id")));
        dto.setProjectName(rs.getString("project_name"));
        return dto;
    }

}
