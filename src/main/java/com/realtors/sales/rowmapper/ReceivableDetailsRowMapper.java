package com.realtors.sales.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import org.springframework.jdbc.core.RowMapper;
import com.realtors.dashboard.dto.ReceivableDetailDTO;

public class ReceivableDetailsRowMapper implements RowMapper<ReceivableDetailDTO> {

	@Override
	public ReceivableDetailDTO mapRow(ResultSet rs, int rowNum) throws SQLException {

		ReceivableDetailDTO dto = new ReceivableDetailDTO();

		dto.setSaleId(rs.getObject("sale_id", UUID.class));

		dto.setProjectId(rs.getObject("project_id", UUID.class));
		dto.setProjectName(rs.getString("project_name"));
		dto.setPlotId(rs.getObject("plot_id", UUID.class));
		dto.setPlotNumber(rs.getString("plot_number"));

		dto.setCustomerId(rs.getObject("customer_id", UUID.class));
		dto.setCustomerName(rs.getString("customer_name"));

		dto.setAgentId(rs.getObject("agent_id", UUID.class));
		dto.setAgentName(rs.getString("agent_name"));

		dto.setSaleAmount(rs.getBigDecimal("sale_amount"));
		dto.setTotalReceived(rs.getBigDecimal("total_received"));
		dto.setOutstandingAmount(rs.getBigDecimal("outstanding_amount"));
		return dto;
	}
}
