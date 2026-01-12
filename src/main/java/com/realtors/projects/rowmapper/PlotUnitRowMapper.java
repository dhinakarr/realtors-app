package com.realtors.projects.rowmapper;


import org.springframework.jdbc.core.RowMapper;

import com.realtors.projects.dto.PlotUnitDto;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class PlotUnitRowMapper implements RowMapper<PlotUnitDto> {
    @Override
    public PlotUnitDto mapRow(ResultSet rs, int rowNum) throws SQLException {

        PlotUnitDto dto = new PlotUnitDto();

        dto.setPlotId((UUID) rs.getObject("plot_id"));
        dto.setProjectId((UUID) rs.getObject("project_id"));
        dto.setPlotNumber(rs.getString("plot_number"));
        dto.setArea(rs.getBigDecimal("area"));
        dto.setRatePerSqft(rs.getBigDecimal("rate_per_sqft"));
        dto.setBasePrice(rs.getBigDecimal("base_price"));
        dto.setRoadWidth(rs.getString("road_width"));
        dto.setSurveyNum(rs.getString("survey_num"));
        dto.setFacing(rs.getString("facing"));
        dto.setWidth(rs.getBigDecimal("width"));
        dto.setBreath(rs.getBigDecimal("breath"));
        dto.setTotalPrice(rs.getBigDecimal("total_price"));
        dto.setIsPrime(rs.getBoolean("is_prime"));
        dto.setStatus(rs.getString("status"));
        dto.setCustomerId((UUID) rs.getObject("customer_id"));
        dto.setRemarks(rs.getString("remarks"));

        return dto;
    }
}

