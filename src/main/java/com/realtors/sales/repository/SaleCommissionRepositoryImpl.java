package com.realtors.sales.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.realtors.sales.dto.SaleCommissionDTO;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SaleCommissionRepositoryImpl implements SaleCommissionRepository {

	private final JdbcTemplate jdbc;

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

}
