package com.realtors.dashboard.repository;

import org.springframework.jdbc.core.RowMapper;

import com.realtors.dashboard.dto.ReceivableDetailDTO;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ReceivableRowMapper implements RowMapper<ReceivableDetailDTO> {

    @Override
    public ReceivableDetailDTO mapRow(ResultSet rs, int rowNum) throws SQLException {

    	ReceivableDetailDTO dto = new ReceivableDetailDTO();

        dto.setSaleId(rs.getObject("sale_id", java.util.UUID.class));
        dto.setProjectId(rs.getObject("project_id", java.util.UUID.class));
        dto.setProjectName(rs.getString("project_name"));

        dto.setPlotId(rs.getObject("plot_id", java.util.UUID.class));
        dto.setPlotNumber(rs.getString("plot_number"));

        dto.setCustomerId(rs.getObject("customer_id", java.util.UUID.class));
        dto.setCustomerName(rs.getString("customer_name"));

        dto.setAgentId(rs.getObject("agent_id", java.util.UUID.class));
        dto.setAgentName(rs.getString("agent_name"));

        dto.setSaleAmount(rs.getBigDecimal("sale_amount"));
        dto.setTotalReceived(rs.getBigDecimal("total_received"));
        dto.setOutstandingAmount(rs.getBigDecimal("outstanding_amount"));

        dto.setConfirmedAt(rs.getTimestamp("confirmed_at").toLocalDateTime());

        return dto;
    }
}
