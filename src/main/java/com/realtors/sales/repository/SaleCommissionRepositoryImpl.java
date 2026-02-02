package com.realtors.sales.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.realtors.sales.dto.SaleCommissionDTO;
import com.realtors.sales.finance.dto.CashFlowItemDTO;
import com.realtors.sales.finance.dto.CashFlowStatus;
import com.realtors.sales.finance.dto.CashFlowType;
import com.realtors.sales.finance.dto.PayableDetailsDTO;
import com.realtors.sales.rowmapper.PayableDetailsRowMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SaleCommissionRepositoryImpl implements SaleCommissionRepository {

	private final JdbcTemplate jdbc;
	private static final Logger logger = LoggerFactory.getLogger(SaleCommissionRepositoryImpl.class);

	@Override
	public List<SaleCommissionDTO> findBySaleId(UUID saleId) {
		String sql = """
				    SELECT commission_id, sale_id, user_id, role_id, percentage, commission_amount
				    FROM sale_commissions
				    WHERE sale_id = ?
				""";

		return jdbc.query(sql, (rs, rowNum) -> {
			SaleCommissionDTO dto = new SaleCommissionDTO();
			dto.setCommissionId(UUID.fromString(rs.getString("commission_id")));
			dto.setSaleId(UUID.fromString(rs.getString("sale_id")));
			dto.setUserId(UUID.fromString(rs.getString("user_id")));
			dto.setRoleId(UUID.fromString(rs.getString("role_id")));
			dto.setPercentage(rs.getBigDecimal("percentage"));
			dto.setCommissionAmount(rs.getBigDecimal("commission_amount"));
			return dto;
		}, saleId);
	}

	@Override
	public List<SaleCommissionDTO> findBySale(UUID saleId, UUID userId) {
		String sql = """
				    SELECT commission_id, sale_id, user_id, role_id, percentage, commission_amount
				    FROM sale_commissions
				    WHERE sale_id = ? AND user_id=?
				""";

		return jdbc.query(sql, (rs, rowNum) -> {
			SaleCommissionDTO dto = new SaleCommissionDTO();
			dto.setCommissionId(UUID.fromString(rs.getString("commission_id")));
			dto.setSaleId(UUID.fromString(rs.getString("sale_id")));
			dto.setUserId(UUID.fromString(rs.getString("user_id")));
			dto.setRoleId(UUID.fromString(rs.getString("role_id")));
			dto.setPercentage(rs.getBigDecimal("percentage"));
			dto.setCommissionAmount(rs.getBigDecimal("commission_amount"));
			return dto;
		}, saleId, userId);
	}

	@Override
	public SaleCommissionDTO insertCommission(SaleCommissionDTO dto) {

		String sql = """
				    INSERT INTO sale_commissions
				    (sale_id, user_id, role_id, percentage, commission_amount)
				    VALUES (?, ?, ?, ?, ?)
				    RETURNING commission_id, sale_id, user_id, role_id,
				              percentage, commission_amount, created_at
				""";

		return jdbc.queryForObject(sql,
				(rs, rowNum) -> new SaleCommissionDTO(rs.getObject("commission_id", UUID.class),
						rs.getObject("sale_id", UUID.class), rs.getObject("user_id", UUID.class),
						rs.getObject("role_id", UUID.class), rs.getBigDecimal("percentage"),
						rs.getBigDecimal("commission_amount"), rs.getTimestamp("created_at")),
				dto.getSaleId(), dto.getUserId(), dto.getRoleId(), dto.getPercentage(), dto.getCommissionAmount());
	}

	@Override
	public void updateCommission(UUID commissionId, BigDecimal percentage, BigDecimal amount) {
		String sql = """
				    UPDATE sale_commissions
				    SET percentage = ?, commission_amount = ?
				    WHERE commission_id = ?
				""";

		jdbc.update(sql, percentage, amount, commissionId);
	}

	@Override
	public BigDecimal getTotalPayable() {
		String sql = """
					SELECT SUM(amount) AS total_paid
				    FROM payments
				    WHERE payment_type = 'RECEIVED'
				""";
		return jdbc.queryForObject(sql, BigDecimal.class);
	}
	
	@Override
	public BigDecimal getPaidThisMonth() {
	    LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
	    LocalDateTime endOfMonth = LocalDateTime.now();

	    String sql = """
	        SELECT COALESCE(SUM(amount),0)
	        FROM payments p
	        JOIN sale_commissions sc ON sc.sale_id = p.sale_id
	        WHERE payment_type='PAID'
	          AND p.payment_date BETWEEN ? AND ?
	    """;

	    return jdbc.queryForObject(sql, BigDecimal.class, startOfMonth, endOfMonth);
	}
	
	@Override
	public BigDecimal getPaidBetween(LocalDateTime from, LocalDateTime to) {
		String sql = """
					SELECT COALESCE(SUM(commission_amount), 0)
					FROM sale_commissions
					WHERE  released_at BETWEEN ? AND ?
				""";
		return jdbc.queryForObject(sql, BigDecimal.class, from, to);
	}

	@Override
	public List<CashFlowItemDTO> getPayables(LocalDate from, LocalDate to) {
		String sql = """
					SELECT
						sc.commission_id,
						sc.sale_id,
						pl.plot_number,
						u.full_name AS agent_name,
						sc.commission_amount,
						sc.created_at
					FROM sale_commissions sc
					JOIN sales s ON s.sale_id = sc.sale_id
					JOIN plot_units pl ON pl.plot_id = s.plot_id
					JOIN app_users u ON u.user_id = sc.user_id
					WHERE sc.is_released = false
				""";

		return jdbc.query(sql, (rs, rowNum) -> {
			CashFlowItemDTO dto = new CashFlowItemDTO();
			dto.setType(CashFlowType.PAYABLE);
			dto.setReferenceId(rs.getObject("commission_id", UUID.class));
			dto.setSaleId(rs.getObject("sale_id", UUID.class));
			dto.setPlotNo(rs.getString("plot_number"));
			dto.setPartyName(rs.getString("agent_name"));
			dto.setAmount(rs.getBigDecimal("commission_amount"));
			dto.setStatus(CashFlowStatus.APPROVED);
			return dto;
		});
	}

	public List<CashFlowItemDTO> findPayables(
	        LocalDate from,
	        LocalDate to,
	        CashFlowStatus status
	) {

	    String sql = """
	        SELECT
	            sc.sale_id,
	            u.full_name AS agent_name,
	            (sc.commission_amount - COALESCE(p.total_paid, 0)) AS outstanding
	        FROM sale_commissions sc
	        JOIN sales s ON s.sale_id = sc.sale_id
	        JOIN app_users u ON u.user_id = sc.user_id
	        LEFT JOIN (
	            SELECT sale_id, SUM(amount) AS total_paid
	            FROM payments
	            WHERE payment_type = 'PAID'
	              AND is_verified = true
	            GROUP BY sale_id
	        ) p ON p.sale_id = sc.sale_id
	        WHERE s.sale_status IN ('CONFIRMED','COMPLETED')
	          AND (sc.commission_amount - COALESCE(p.total_paid, 0)) > 0
	    """;

	    List<CashFlowItemDTO> items = jdbc.query(
	        sql,
	        (rs, rowNum) -> {
	            CashFlowItemDTO dto = new CashFlowItemDTO();
	            dto.setType(CashFlowType.PAYABLE);
	            dto.setSaleId(rs.getObject("sale_id", UUID.class));
	            dto.setPartyName(rs.getString("agent_name"));
	            dto.setAmount(rs.getBigDecimal("outstanding"));
				dto.setStatus(dto.getAmount().compareTo(BigDecimal.ZERO) > 0 ? CashFlowStatus.OVERDUE : CashFlowStatus.DUE );
				
	            return dto;
	        }
	    );
	    if (status == null) return items;
	    return items.stream()
	        .filter(i -> i.getStatus() == status)
	        .toList();
	}

	public List<PayableDetailsDTO> getPayableDetails() {
		String sql = "SELECT * FROM v_commission_payable_details WHERE commission_payable > 0";
		return jdbc.query(sql, new PayableDetailsRowMapper());
	}
	
	public List<PayableDetailsDTO> getPayableDetails(LocalDate from, LocalDate to) {
		String sql = "SELECT * FROM v_commission_payable_details WHERE commission_payable > 0 AND sale_date BETWEEN ? AND ?";
		return jdbc.query(sql, new PayableDetailsRowMapper(), from, to);
	}

	@Override
	public void updateStatus(UUID saleId, UUID userId, String status, boolean released) {
		String sql = """
			    UPDATE sale_commissions
			    SET status=?, is_released = ?, released_at = now()
			    WHERE sale_id = ?
			""";
logger.info("@SaleCommissionRepositoryImpl.updateStatus saleId: {}", saleId);
		jdbc.update(sql, status, released, saleId);
		
	}

	@Override
	public void handleCommissionReversal(UUID saleId) {
	}

	@Override
	public BigDecimal getTotalCommission(UUID saleId, UUID userId) {
		String sql = """
				SELECT commission_amount
				FROM sale_commissions
				WHERE  sale_id=? AND user_id=?
			""";
	return jdbc.queryForObject(sql, BigDecimal.class, saleId, userId);
	}

	@Override
	public void deleteCommissionData(UUID saleId, UUID userId) {
		String sql = """
				DELETE FROM sale_commissions
				WHERE  sale_id=? AND user_id=?
			""";
		jdbc.update(sql, saleId, userId);
	}

	@Override
	public void deleteBySaleId(UUID saleId) {
		String sql = """
				DELETE FROM sale_commissions
				WHERE  sale_id=? 
			""";
		jdbc.update(sql, saleId);
	}


}
