package com.realtors.projects.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.jdbc.core.RowMapper;

import com.realtors.projects.dto.ProjectDto;

public class ProjectRowMapper implements RowMapper<ProjectDto> {
	
    @Override
    public ProjectDto mapRow(ResultSet rs, int rowNum) throws SQLException {
        ProjectDto dto = new ProjectDto();
        dto.setProjectId((UUID) rs.getObject("project_id"));
        dto.setProjectName(rs.getString("project_name"));
        dto.setLocationDetails(rs.getString("location_details"));
        dto.setSurveyNumber(rs.getString("survey_number"));
        dto.setStartDate(rs.getObject("start_date", LocalDate.class));
        dto.setEndDate(rs.getObject("end_date", LocalDate.class));
        dto.setNoOfPlots(rs.getInt("no_of_plots"));
        dto.setPricePerSqft(rs.getBigDecimal("price_per_sqft"));
        dto.setRegCharges(rs.getBigDecimal("reg_charges"));
        dto.setDocCharges(rs.getBigDecimal("doc_charges"));
        dto.setOtherCharges(rs.getBigDecimal("other_charges"));
        dto.setGuidanceValue(rs.getBigDecimal("guidance_value"));
        dto.setStatus(rs.getString("status"));
        
        return dto;
    }
}

