package com.realtors.sales.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.jdbc.core.RowMapper;

import com.realtors.sales.dto.PaymentDTO;

public class PaymentRowMapper implements RowMapper<PaymentDTO> {

    @Override
    public PaymentDTO mapRow(ResultSet rs, int rowNum) throws SQLException {
        PaymentDTO dto = new PaymentDTO();
        dto.setPaymentId(UUID.fromString(rs.getString("payment_id")));
        dto.setSaleId(UUID.fromString(rs.getString("sale_id")));
        dto.setAmount(rs.getBigDecimal("amount"));

        Timestamp ts = rs.getTimestamp("payment_date");
        dto.setPaymentDate(rs.getObject("payment_date", LocalDate.class));

        dto.setPaymentMode(rs.getString("payment_mode"));
        dto.setTransactionRef(rs.getString("transaction_ref"));
        dto.setRemarks(rs.getString("remarks"));

        return dto;
    }
}
