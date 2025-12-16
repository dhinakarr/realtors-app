package com.realtors.sales.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import org.springframework.jdbc.core.RowMapper;

import com.realtors.sales.finance.dto.PayableDetailsDTO;

public class PayableDetailsRowMapper implements RowMapper<PayableDetailsDTO> {

	@Override
	public PayableDetailsDTO mapRow(ResultSet rs, int rowNum) throws SQLException {

		PayableDetailsDTO dto = new PayableDetailsDTO();

		dto.setSaleId(rs.getObject("sale_id", UUID.class));
		dto.setProjectId(rs.getObject("project_id", UUID.class));
		dto.setProjectName(rs.getString("project_name"));
		dto.setPlotId(rs.getObject("plot_id", UUID.class));
		dto.setPlotNumber(rs.getString("plot_number"));
		dto.setAgentId(rs.getObject("agent_id", UUID.class));
		dto.setAgentName(rs.getString("agent_name"));

		dto.setSaleAmount(rs.getBigDecimal("sale_amount"));
		dto.setTotalCommission(rs.getBigDecimal("total_commission"));
		dto.setCustomerPaid(rs.getBigDecimal("customer_paid"));
		dto.setCommissionPaid(rs.getBigDecimal("commission_paid"));
		dto.setCommissionEligible(rs.getBigDecimal("commission_eligible"));
		dto.setCommissionPayable(rs.getBigDecimal("commission_payable"));
		dto.setSaleStatus(rs.getString("sale_status"));
		return dto;
	}
}

