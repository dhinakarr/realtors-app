package com.realtors.sales.repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.realtors.sales.dto.PaymentDTO;
import com.realtors.sales.rowmapper.PaymentRowMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PaymentRepositoryImpl {

	private final JdbcTemplate jdbc;

	public PaymentDTO save(PaymentDTO dto) {

		String sql = """
				    INSERT INTO payments
				    (payment_type, sale_id, amount, payment_date, payment_mode,
				     transaction_ref, remarks, collected_by, paid_to, is_verified)
				    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
				    RETURNING payment_id, sale_id, payment_type, amount, payment_date,
				              payment_mode, transaction_ref, remarks, collected_by, paid_to, is_verified
				""";

		return jdbc.queryForObject(sql, (rs, rowNum) -> {
			PaymentDTO p = new PaymentDTO();
//			p.setPaymentId((UUID) rs.getObject("payment_id"));
			p.setSaleId((UUID) rs.getObject("sale_id"));
			p.setPaymentType(rs.getString("payment_type"));
			p.setAmount(rs.getBigDecimal("amount"));
			p.setPaymentDate(rs.getTimestamp("payment_date").toLocalDateTime());
			p.setPaymentMode(rs.getString("payment_mode"));
			p.setTransactionRef(rs.getString("transaction_ref"));
			p.setRemarks(rs.getString("remarks"));
			p.setCollectedBy((UUID) rs.getObject("collected_by"));
			p.setPaidTo((UUID) rs.getObject("paid_to"));
			p.setVerified(rs.getBoolean("is_verified"));
			return p;
		}, dto.getPaymentType(), dto.getSaleId(), dto.getAmount(), Timestamp.valueOf(dto.getPaymentDate()),
				dto.getPaymentMode(), dto.getTransactionRef(), dto.getRemarks(), dto.getCollectedBy(), dto.getPaidTo(),
				dto.isVerified());
	}
	
	

	public List<PaymentDTO> getBySaleId(UUID saleId) {
		String sql = """
				    SELECT payment_id, sale_id, amount, payment_date, payment_mode, transaction_ref, remarks
				    FROM payments
				    WHERE sale_id = ?
				    ORDER BY payment_date ASC
				""";
		return jdbc.query(sql, new PaymentRowMapper(), saleId);
	}

	public List<PaymentDTO> findBySaleId(UUID saleId) {
		String sql = "SELECT * FROM payments WHERE sale_id = ?";
		return jdbc.query(sql, new BeanPropertyRowMapper<>(PaymentDTO.class), saleId);
	}

	public BigDecimal getTotalReceived(UUID saleId) {
		String sql = "SELECT COALESCE(SUM(amount),0) FROM payments WHERE sale_id = ? AND payment_type = 'RECEIVED'";
		return jdbc.queryForObject(sql, BigDecimal.class, saleId);
	}

	public BigDecimal getTotalPaid(UUID saleId) {
		String sql = "SELECT COALESCE(SUM(amount),0) FROM payments WHERE sale_id = ? AND payment_type = 'PAID'";
		return jdbc.queryForObject(sql, BigDecimal.class, saleId);
	}
}
