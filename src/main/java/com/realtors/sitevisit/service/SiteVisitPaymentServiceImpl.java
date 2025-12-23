package com.realtors.sitevisit.service;

import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.realtors.sitevisit.dto.SitePaymentDTO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SiteVisitPaymentServiceImpl implements SiteVisitPaymentService {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void savePayments(UUID siteVisitId, List<SitePaymentDTO> payments) {

        if (payments == null) return;

        for (SitePaymentDTO p : payments) {
            jdbcTemplate.update("""
                INSERT INTO site_visit_payments
                (payment_id, site_visit_id, user_id,
                 amount, payment_mode, payment_date, remarks)
                VALUES (?, ?, ?, ?, ?, ?, ?)
            """,
                UUID.randomUUID(),
                siteVisitId,
                p.getUserId(),
                p.getAmount(),
                p.getPaymentMode(),
                p.getPaymentDate(),
                p.getRemarks()
            );
        }
    }
}
