package com.realtors.dashboard.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.realtors.dashboard.dto.CommissionDetailsDTO;
import com.realtors.dashboard.dto.CommissionSummaryDTO;

@Repository
public class CommissionRepository {

    private final JdbcTemplate jdbcTemplate;

    public CommissionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Summary cards */
    public CommissionSummaryDTO getSummary(UUID agentId) {

        String sql = """
            SELECT
                COALESCE(SUM(total_commission), 0)  AS total_commission,
                COALESCE(SUM(commission_paid), 0)   AS total_paid,
                COALESCE(SUM(commission_payable), 0) AS total_payable
            FROM v_commission_payable_details
            WHERE (:agentId IS NULL OR agent_id = :agentId)
        """;

        return jdbcTemplate.queryForObject(
            sql,
            
            (rs, rn) -> {
                CommissionSummaryDTO dto = new CommissionSummaryDTO();
                dto.setTotalCommission(rs.getBigDecimal("total_commission"));
                dto.setTotalPaid(rs.getBigDecimal("total_paid"));
                dto.setTotalPayable(rs.getBigDecimal("total_payable"));
                return dto;
            },
            agentId
        );
    }

    /** Table */
    public List<CommissionDetailsDTO> getCommissions(UUID agentId) {

        String sql = """
            SELECT *
            FROM v_commission_payable_details
            WHERE (:agentId IS NULL OR agent_id = :agentId)
            ORDER BY confirmed_at DESC
        """;

        return jdbcTemplate.query(sql, new CommissionRowMapper(), agentId);
    }
}
