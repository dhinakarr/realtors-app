package com.realtors.dashboard.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.realtors.dashboard.dto.ReceivableDTO;
import com.realtors.dashboard.dto.ReceivableSummaryDTO;

@Repository
public class ReceivableRepository {

    private final JdbcTemplate jdbcTemplate;

    public ReceivableRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Summary cards */
    public ReceivableSummaryDTO getSummary() {

        String sql = """
            SELECT
                COALESCE(SUM(sale_amount), 0)      AS total_sale,
                COALESCE(SUM(total_received), 0)   AS total_received,
                COALESCE(SUM(outstanding_amount), 0) AS total_outstanding
            FROM v_receivable_details
        """;

        return jdbcTemplate.queryForObject(sql, (rs, rn) -> {
            ReceivableSummaryDTO dto = new ReceivableSummaryDTO();
            dto.setTotalSaleAmount(rs.getBigDecimal("total_sale"));
            dto.setTotalReceived(rs.getBigDecimal("total_received"));
            dto.setTotalOutstanding(rs.getBigDecimal("total_outstanding"));
            return dto;
        });
    }

    /** Table */
    public List<ReceivableDTO> getReceivables(UUID agentId) {

        String sql = """
            SELECT *
            FROM v_receivable_details
            WHERE (:agentId IS NULL OR agent_id = :agentId)
            ORDER BY confirmed_at DESC
        """;

        return jdbcTemplate.query(sql, new ReceivableRowMapper(), agentId);
    }
}
