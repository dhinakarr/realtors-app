package com.realtors.dashboard.repository;

import org.springframework.jdbc.core.RowMapper;

import com.realtors.dashboard.dto.CommissionDetailsDTO;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;

public class CommissionRowMapper implements RowMapper<CommissionDetailsDTO> {

    @Override
    public CommissionDetailsDTO mapRow(ResultSet rs, int rowNum) throws SQLException {

        CommissionDetailsDTO dto = new CommissionDetailsDTO();

        dto.setCommissionId(rs.getObject("commission_id", java.util.UUID.class));
        dto.setSaleId(rs.getObject("sale_id", java.util.UUID.class));

        dto.setAgentId(rs.getObject("agent_id", java.util.UUID.class));
        dto.setAgentName(rs.getString("agent_name"));

        dto.setProjectId(rs.getObject("project_id", java.util.UUID.class));
        dto.setProjectName(rs.getString("project_name"));

        dto.setPlotId(rs.getObject("plot_id", java.util.UUID.class));
        dto.setPlotNumber(rs.getString("plot_number"));
        dto.setBaseAmount(rs.getBigDecimal("base_amount"));
        dto.setSaleAmount(rs.getBigDecimal("sale_amount"));
        dto.setCustomerPaid(rs.getBigDecimal("customer_paid"));

        dto.setTotalCommission(rs.getBigDecimal("total_commission"));
        dto.setCommissionPaid(rs.getBigDecimal("commission_paid"));
        dto.setCommissionEligible(rs.getBigDecimal("commission_eligible"));
        dto.setCommissionPayable(rs.getBigDecimal("commission_payable"));
        
        dto.setSaleStatus(rs.getString("sale_status"));
//        dto.setConfirmedAt(rs.getObject("confirmed_at", java.time.LocalDate.class));
        java.time.OffsetDateTime confirmed = rs.getObject("confirmed_at", java.time.OffsetDateTime.class);
        java.time.OffsetDateTime saleDate = rs.getObject("sale_date", java.time.OffsetDateTime.class);
        java.time.OffsetDateTime paymentDate = rs.getObject("payment_date", java.time.OffsetDateTime.class);
        
        // 2. Convert to LocalDate for your DTO
        if (confirmed != null) {
            dto.setConfirmedAt(confirmed.toLocalDate());
        }
        if (saleDate != null) {
            dto.setSaleDate(saleDate.toLocalDate());
        }
        if (paymentDate != null) {
            dto.setPaymentDate(paymentDate.toLocalDate());
        }
        return dto;
    }
}
