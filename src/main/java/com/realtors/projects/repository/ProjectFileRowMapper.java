package com.realtors.projects.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import org.springframework.jdbc.core.RowMapper;

import com.realtors.projects.dto.ProjectFileDto;

public class ProjectFileRowMapper implements RowMapper<ProjectFileDto> {
	
    @Override
    public ProjectFileDto mapRow(ResultSet rs, int rowNum) throws SQLException {
    	ProjectFileDto dto = new ProjectFileDto();
        dto.setProjectFileId((UUID) rs.getObject("project_file_id"));
        dto.setProjectId((UUID)rs.getObject("project_id"));
        dto.setFilePath(rs.getString("file_path"));
        dto.setFileName(rs.getString("file_name"));
        dto.setSizeByts(rs.getInt("size_bytes"));
        return dto;
    }
}


