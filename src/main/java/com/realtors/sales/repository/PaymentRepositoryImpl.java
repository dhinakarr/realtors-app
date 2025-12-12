package com.realtors.sales.repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.realtors.sales.dto.PaymentDTO;
import com.realtors.sales.rowmapper.PaymentRowMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PaymentRepositoryImpl implements PaymentRepository {

    private final JdbcTemplate jdbc;

    @Override
    public PaymentDTO insertPayment(PaymentDTO payment) {
        String sql = """
            INSERT INTO payments
            (sale_id, amount, payment_date, payment_mode, transaction_ref, remarks)
            VALUES (?, ?, ?, ?, ?, ?)
            RETURNING payment_id, sale_id,  amount, payment_date, payment_mode, transaction_ref, remarks
        """;

        return jdbc.queryForObject(sql, new PaymentRowMapper(),
                payment.getSaleId(),
                payment.getAmount(),
                Timestamp.valueOf(payment.getPaymentDate() != null ? payment.getPaymentDate() : LocalDateTime.now()),
                payment.getPaymentMode(),
                payment.getTransactionRef(),
                payment.getRemarks()
        );
    }

    @Override
    public List<PaymentDTO> findBySaleId(UUID saleId) {
        String sql = """
            SELECT payment_id, sale_id, amount, payment_date, payment_mode, transaction_ref, remarks
            FROM payments
            WHERE sale_id = ?
            ORDER BY payment_date ASC
        """;
        return jdbc.query(sql, new PaymentRowMapper(), saleId);
    }
}

