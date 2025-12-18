package com.realtors.sales.repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.realtors.sales.dto.PaymentDTO;
import com.realtors.sales.dto.ProjectWiseTotalReceivable;
import com.realtors.sales.dto.SaleWiseReceivable;
import com.realtors.sales.finance.dto.ReceivableDetailsDTO;
import com.realtors.sales.rowmapper.PaymentRowMapper;
import com.realtors.sales.rowmapper.ReceivableDetailsRowMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PaymentRepositoryImpl {

	private final JdbcTemplate jdbc;

	public PaymentDTO save(PaymentDTO dto) {
		dto.setVerified(true);
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
			p.setPaymentId((UUID) rs.getObject("payment_id"));
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

	public PaymentDTO getById(UUID paymentId) {
		String sql = """
				    SELECT payment_id, sale_id, amount, payment_date, payment_mode, transaction_ref, remarks
				    FROM payments
				    WHERE payment_id = ?
				""";
		return jdbc.queryForObject(sql, new PaymentRowMapper(), paymentId);
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
	
	public BigDecimal getTotalPaidThisMonth() {
		 LocalDateTime from = LocalDate.now().withDayOfMonth(1).atStartOfDay();
		 LocalDateTime to = LocalDateTime.now();
		String sql = "SELECT COALESCE(SUM(amount),0) FROM payments WHERE payment_type = 'PAID' AND payment_date BETWEEN ? AND ?" ;
		return jdbc.queryForObject(sql, BigDecimal.class, from, to);
	}
	
	public BigDecimal getTotalPaid() {
		String sql = "SELECT COALESCE(SUM(amount),0) FROM payments WHERE payment_type = 'PAID'" ;
		return jdbc.queryForObject(sql, BigDecimal.class);
	}
	
	public BigDecimal getTotalReceivables() {
		String sql = """
		        SELECT COALESCE(SUM(s.total_price), 0) - COALESCE(SUM(p.total_received), 0) AS total_receivable
					FROM sales s
					LEFT JOIN ( SELECT sale_id,  SUM(amount) AS total_received
					    FROM payments
					    WHERE payment_type = 'RECEIVED'
					    GROUP BY sale_id
					) p ON p.sale_id = s.sale_id
					WHERE s.sale_status in ( 'BOOKED', 'IN_PROGRESS');
		    """;
		    return jdbc.queryForObject(sql, BigDecimal.class);
	}
	
	public PaymentDTO update(PaymentDTO dto) {
	    String sql = """
	        UPDATE payments
	        SET amount = ?,
	            payment_date = ?,
	            payment_mode = ?,
	            transaction_ref = ?,
	            remarks = ?,
	            updated_at = now()
	        WHERE payment_id = ?
	          AND deleted = false
	        RETURNING payment_id, sale_id, amount, payment_date,
	                  payment_mode, transaction_ref, remarks,
	                  status, is_verified
	    """;

	    return jdbc.queryForObject(sql, new PaymentRowMapper(),
	        dto.getAmount(),
	        Timestamp.valueOf(dto.getPaymentDate()),
	        dto.getPaymentMode(),
	        dto.getTransactionRef(),
	        dto.getRemarks(),
	        dto.getPaymentId()
	    );
	}
	
	public void softDelete(UUID paymentId, UUID actorUserId) {
	    String sql = """
	        UPDATE payments
	        SET deleted = true,
	            updated_at = now()
	        WHERE payment_id = ?
	    """;
	    jdbc.update(sql, paymentId);
	}

	public PaymentDTO verify(UUID paymentId, UUID verifierId) {
	    String sql = """
	        UPDATE payments
	        SET status = 'VERIFIED',
	            is_verified = true,
	            verified_by = ?,
	            verified_at = now()
	        WHERE payment_id = ?
	        RETURNING *
	    """;
	    return jdbc.queryForObject(sql, new PaymentRowMapper(), verifierId, paymentId);
	}
	
	public BigDecimal getReceivedBetween(LocalDateTime from, LocalDateTime to) {
		String sql = """
			SELECT COALESCE(SUM(amount), 0)
			FROM payments
			WHERE payment_type = 'RECEIVED'
		""";
		return jdbc.queryForObject(sql, BigDecimal.class);
	}
	
	public SaleWiseReceivable getSaleWiseReceivable() {
		String sql = """
				SELECT s.sale_id, s.total_price, COALESCE(p.total_received, 0) AS total_received,
					    (s.total_price - COALESCE(p.total_received, 0)) AS outstanding_amount
					FROM sales s
					LEFT JOIN (SELECT sale_id, SUM(amount) AS total_received
					    FROM payments
					    WHERE payment_type = 'RECEIVED'
					    GROUP BY sale_id
					) p ON p.sale_id = s.sale_id
					WHERE s.sale_status = 'BOOKED'
				""";
		return jdbc.queryForObject(sql, SaleWiseReceivable.class);
	}
	
	public ProjectWiseTotalReceivable getProjectWiseReceivable() {
		String sql = """
				SELECT s.project_id, SUM(s.total_price) AS total_sales,
						    COALESCE(SUM(p.total_received), 0) AS total_received,
						    (SUM(s.total_price) - COALESCE(SUM(p.total_received), 0)) AS total_receivable
						FROM sales s
						LEFT JOIN (SELECT sale_id, SUM(amount) AS total_received
						    FROM payments
						    WHERE payment_type = 'RECEIVED'
						    GROUP BY sale_id
						) p ON p.sale_id = s.sale_id
						WHERE s.sale_status = 'BOOKED'
						GROUP BY s.project_id
				""";
		return jdbc.queryForObject(sql, ProjectWiseTotalReceivable.class);
	}
	
	public List<ReceivableDetailsDTO> getReceivableDetails() {
		String sql = "SELECT * FROM v_receivable_details WHERE outstanding_amount > 0";
		return jdbc.query(sql, new ReceivableDetailsRowMapper());
	}
	
	public List<ReceivableDetailsDTO> findPendingByProject(UUID projectId) {
	    String sql = "SELECT *  FROM v_receivable_details WHERE outstanding_amount > 0 AND project_id = ? ";
	    return jdbc.query( sql, new ReceivableDetailsRowMapper(), projectId);
	}
	
	public List<ReceivableDetailsDTO> findPendingByCustomer(UUID customerId) {
	    String sql = "SELECT *  FROM v_receivable_details WHERE outstanding_amount > 0 AND customer_id = ? ";
	    return jdbc.query( sql, new ReceivableDetailsRowMapper(), customerId);
	}
	
	public List<ReceivableDetailsDTO> findPendingByAgent(UUID userId) {
	    String sql = "SELECT *  FROM v_receivable_details WHERE outstanding_amount > 0 AND agent_id = ?";
	    return jdbc.query( sql, new ReceivableDetailsRowMapper(), userId);
	}
	
	public List<ReceivableDetailsDTO> findPendingPaged(int limit, int offset) {
		String sql = "SELECT *  FROM v_receivable_details WHERE outstanding_amount > 0 AND agent_id = ?  LIMIT ? OFFSET ?";
	    return jdbc.query( sql, new ReceivableDetailsRowMapper(), limit, offset);
	}
	
	public BigDecimal getOutstandingDueToday() {
		String sql = """
			SELECT COALESCE(SUM(
				s.total_price - COALESCE(p.total_received, 0)
			), 0)
			FROM sales s
			LEFT JOIN (
				SELECT sale_id, SUM(amount) total_received
				FROM payments
				WHERE payment_type = 'RECEIVED'
				  AND is_verified = true
				GROUP BY sale_id
			) p ON p.sale_id = s.sale_id
		""";

		return jdbc.queryForObject(sql, BigDecimal.class);
	}

	public BigDecimal getTotalReceivedAll() {
	    String sql = """
	        SELECT COALESCE(SUM(amount), 0)
	        FROM payments
	        WHERE payment_type = 'RECEIVED'
	          AND is_verified = true
	    """;
	    return jdbc.queryForObject(sql, BigDecimal.class);
	}

}
