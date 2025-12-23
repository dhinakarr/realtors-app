package com.realtors.sitevisit.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.realtors.sitevisit.dto.SiteVisitPatchDTO;
import com.realtors.sitevisit.dto.SiteVisitRequestDTO;
import com.realtors.sitevisit.dto.SiteVisitResponseDTO;
import com.realtors.sitevisit.mapper.SiteVisitRowMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class SiteVisitRepository {

	private final JdbcTemplate jdbcTemplate;

	public UUID create(SiteVisitRequestDTO dto) {
		UUID id = UUID.randomUUID();

		jdbcTemplate.update("""
				    INSERT INTO site_visits
				    (site_visit_id, visit_date, user_id, project_id,
				     vehicle_type, expense_amount, remarks, status)
				    VALUES (?, ?, ?, ?, ?, ?, ?, 'OPEN')
				""", id, dto.getVisitDate(), dto.getUserId(), dto.getProjectId(), dto.getVehicleType(),
				dto.getExpenseAmount(), dto.getRemarks());

		return id;
	}

	public SiteVisitResponseDTO findById(UUID visitId) {
		StringBuilder sql = selectQuery();
		sql.append(" AND sv.site_visit_id = ?");
		sql.append(
				"""
						    GROUP BY sv.site_visit_id, sv.visit_date, sv.user_id, u.full_name, sv.project_id, p.project_name,  sv.vehicle_type,
						        sv.expense_amount, sv.remarks,  acc.total_paid, acc.balance, acc.status
						    ORDER BY sv.visit_date DESC
						""");
		SiteVisitResponseDTO dto = jdbcTemplate.query(sql.toString(), new SiteVisitRowMapper(), visitId).get(0);
		return dto;
	}

	public void patch(UUID siteVisitId, SiteVisitPatchDTO dto) {

		StringBuilder sql = new StringBuilder("UPDATE site_visits SET ");
		List<Object> params = new ArrayList<>();

		if (dto.getVisitDate() != null) {
			sql.append("visit_date = ?, ");
			params.add(dto.getVisitDate());
		}
		if (dto.getProjectId() != null) {
			sql.append("project_id = ?, ");
			params.add(dto.getProjectId());
		}
		if (dto.getVehicleType() != null) {
			sql.append("vehicle_type = ?, ");
			params.add(dto.getVehicleType());
		}
		if (dto.getExpenseAmount() != null) {
			sql.append("expense_amount = ?, ");
			params.add(dto.getExpenseAmount());
		}
		if (dto.getRemarks() != null) {
			sql.append("remarks = ?, ");
			params.add(dto.getRemarks());
		}
		sql.append("updated_at = now() ");
		sql.append("WHERE site_visit_id = ?");
		params.add(siteVisitId);
		jdbcTemplate.update(sql.toString(), params.toArray());
	}

	public List<SiteVisitResponseDTO> list(UUID userId, UUID projectId, LocalDate fromDate, LocalDate toDate) {
		StringBuilder sql = selectQuery();
		List<Object> params = new ArrayList<>();

		if (userId != null) {
			sql.append(" AND sv.user_id = ? ");
			params.add(userId);
		}

		if (projectId != null) {
			sql.append(" AND sv.project_id = ? ");
			params.add(projectId);
		}

		if (fromDate != null) {
			sql.append(" AND sv.visit_date >= ? ");
			params.add(fromDate);
		}

		if (toDate != null) {
			sql.append(" AND sv.visit_date <= ? ");
			params.add(toDate);
		}
		sql.append("""
				    GROUP BY
				        sv.site_visit_id,  sv.visit_date, sv.user_id, u.full_name, sv.project_id, p.project_name,
				        sv.vehicle_type, sv.expense_amount,  sv.remarks, acc.total_paid, acc.balance, acc.status
				    ORDER BY sv.visit_date DESC
				""");
		return jdbcTemplate.query(sql.toString(), params.toArray(), new SiteVisitRowMapper());
	}

	public BigDecimal getExpenseAmount(UUID siteVisitId) {
		return jdbcTemplate.queryForObject("""
				    SELECT expense_amount
				    FROM site_visits
				    WHERE site_visit_id = ?
				""", BigDecimal.class, siteVisitId);
	}

	public void delete(UUID siteVisitId) {
		jdbcTemplate.update("DELETE FROM site_visit_customers WHERE site_visit_id = ?", siteVisitId);
		jdbcTemplate.update("DELETE FROM site_visit_accounts WHERE site_visit_id = ?", siteVisitId);
		jdbcTemplate.update("DELETE FROM site_visit_payments WHERE site_visit_id = ?", siteVisitId);
		jdbcTemplate.update("DELETE FROM site_visits WHERE site_visit_id = ?", siteVisitId);
	}

	private StringBuilder selectQuery() {
		StringBuilder sql = new StringBuilder("""
				    SELECT  sv.site_visit_id,  sv.visit_date, sv.user_id,  u.full_name AS username,  sv.project_id,  p.project_name AS project_name,
				        sv.vehicle_type, sv.expense_amount, sv.remarks, acc.total_paid, acc.balance, acc.status,
				        STRING_AGG(c.customer_name, ', ' ORDER BY c.customer_name) AS customer_names
				    FROM site_visits sv
				    JOIN site_visit_accounts acc ON acc.site_visit_id = sv.site_visit_id
				    JOIN app_users u ON u.user_id = sv.user_id
				    JOIN projects p  ON p.project_id = sv.project_id
				    LEFT JOIN site_visit_customers svc ON svc.site_visit_id = sv.site_visit_id
				    LEFT JOIN customers c  ON c.customer_id = svc.customer_id
				    WHERE 1=1
				""");
		return sql;
	}

}
