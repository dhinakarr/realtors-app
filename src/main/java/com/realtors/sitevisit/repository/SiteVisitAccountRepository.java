package com.realtors.sitevisit.repository;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SiteVisitAccountRepository {

	private final JdbcTemplate jdbcTemplate;

	public void create(UUID siteVisitId, BigDecimal expenseAmount) {
		jdbcTemplate.update("""
				    INSERT INTO site_visit_accounts
				    (site_visit_id, total_paid, balance, status)
				    VALUES (?, 0, ?, 'OPEN')
				""", siteVisitId, expenseAmount);
	}

	public void updateTotals(UUID siteVisitId, BigDecimal totalPaid, BigDecimal balance, String status) {

		jdbcTemplate.update("""
				    UPDATE site_visit_accounts
				    SET total_paid = ?, balance = ?, status = ?
				    WHERE site_visit_id = ?
				""", totalPaid, balance, status, siteVisitId);
	}
	
	public BigDecimal getBalanceAmount(UUID siteVisitId) {
		String sql = "select balance from site_visit_accounts where site_visit_id=?::uuid";
		return jdbcTemplate.queryForObject(sql, BigDecimal.class, siteVisitId);
	}
}
