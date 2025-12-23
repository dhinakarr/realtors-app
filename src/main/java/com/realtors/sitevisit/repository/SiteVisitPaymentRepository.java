package com.realtors.sitevisit.repository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.realtors.sitevisit.dto.PaymentPatchDTO;
import com.realtors.sitevisit.dto.SitePaymentDTO;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SiteVisitPaymentRepository {

    private final JdbcTemplate jdbcTemplate;

    public void insert(UUID siteVisitId, SitePaymentDTO dto) {

        jdbcTemplate.update("""
            INSERT INTO site_visit_payments
            (payment_id, site_visit_id, user_id,
             amount, payment_mode, payment_date, remarks)
            VALUES (?, ?, ?, ?, ?, CURRENT_DATE, ?)
        """,
            UUID.randomUUID(),
            siteVisitId,
            dto.getUserId(),
            dto.getAmount(),
            dto.getPaymentMode(),
            dto.getRemarks()
        );
    }

    public BigDecimal getTotalPaid(UUID siteVisitId) {
        return jdbcTemplate.queryForObject("""
            SELECT COALESCE(SUM(amount), 0)
            FROM site_visit_payments
            WHERE site_visit_id = ?
        """, BigDecimal.class, siteVisitId);
    }
    
    public List<SitePaymentDTO> listBySiteVisit(UUID siteVisitId) {
        return jdbcTemplate.query("""
            SELECT payment_id, user_id, amount, payment_mode, payment_date, remarks
            FROM site_visit_payments
            WHERE site_visit_id = ?
            ORDER BY payment_date DESC
        """,
        (rs, rowNum) -> new SitePaymentDTO(
            rs.getObject("payment_id", UUID.class),
            rs.getObject("user_id", UUID.class),
            rs.getBigDecimal("amount"),
            rs.getString("payment_mode"),
            rs.getDate("payment_date").toLocalDate(),
            rs.getString("remarks")
        ),
        siteVisitId);
    }
    
    public void patch(UUID paymentId, PaymentPatchDTO dto) {
        StringBuilder sql = new StringBuilder("UPDATE site_visit_payments SET ");
        List<Object> params = new ArrayList<>();

        if (dto.getAmount() != null) {
            sql.append("amount = ?, ");
            params.add(dto.getAmount());
        }
        if (dto.getPaymentMode() != null) {
            sql.append("payment_mode = ?, ");
            params.add(dto.getPaymentMode());
        }
        if (dto.getRemarks() != null) {
            sql.append("remarks = ?, ");
            params.add(dto.getRemarks());
        }
        sql.append("updated_at = now() WHERE payment_id = ?");
        params.add(paymentId);
        jdbcTemplate.update(sql.toString(), params.toArray());
    }
    				
    public void delete(UUID paymentId, UUID visitId) {
        jdbcTemplate.update("DELETE FROM site_visit_payments WHERE payment_id = ? AND site_visit_id=?", paymentId, visitId);
    }
}
