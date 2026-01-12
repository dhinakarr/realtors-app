package com.realtors.dashboard.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.realtors.dashboard.dto.CommissionDetailsDTO;
import com.realtors.dashboard.dto.CommissionSummaryDTO;

@Repository
public class CommissionRepository {

    private final JdbcTemplate jdbcTemplate;
    
    private static final RowMapper<CommissionDetailsDTO> ROW_MAPPER  =  new BeanPropertyRowMapper<>(CommissionDetailsDTO.class);

    public CommissionRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** Summary cards */
    public CommissionSummaryDTO getSummary(UUID agentId) {

        String sql = """
        		SELECT 
				    SUM(total_commission)              AS total_commission,
				    COALESCE(SUM(paid_amount), 0)       AS total_paid,
				    SUM(total_commission)
				      - COALESCE(SUM(paid_amount), 0)   AS total_payable
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
    
    public List<CommissionDetailsDTO> getCommissionsPaid(LocalDateTime from, LocalDateTime to) {

        String sql = """
            SELECT *
            FROM v_commission_payable_details_payments
            WHERE payment_date >= ?
		    AND payment_date < ?
            ORDER BY confirmed_at DESC
        """;
        return jdbcTemplate.query(sql, ROW_MAPPER, from, to);
    }
    
}
