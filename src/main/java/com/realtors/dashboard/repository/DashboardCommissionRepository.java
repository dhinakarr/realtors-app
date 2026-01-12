package com.realtors.dashboard.repository;

import java.util.List;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.realtors.dashboard.dto.CommissionSummaryDTO;
import com.realtors.dashboard.dto.DashboardScope;

@Repository
public class DashboardCommissionRepository {

	private final NamedParameterJdbcTemplate jdbc;
	private static final Logger logger = LoggerFactory.getLogger(DashboardCommissionRepository.class);

	public DashboardCommissionRepository(NamedParameterJdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	public List<CommissionSummaryDTO> fetchCommissionSummary(DashboardScope scope) {
		StringBuilder sql = new StringBuilder("""
				    SELECT agent_id,
				           agent_name,
				           SUM(total_commission)   AS total_commission,
				           SUM(commission_paid)    AS commission_paid,
				           SUM(commission_payable) AS commission_payable
				    FROM v_commission_payable_details
				""");

		MapSqlParameterSource params = new MapSqlParameterSource();
		List<String> conditions = new ArrayList<>();

		if (!scope.isAll()) {
			if (scope.getUserIds() != null && !scope.getUserIds().isEmpty()) {
			    conditions.add("agent_id IN (:userIds)");
			    params.addValue("userIds", scope.getUserIds());
			}
			if (scope.hasProjectScope() && scope.getProjectIds() != null && !scope.getProjectIds().isEmpty()) {
				conditions.add("project_id IN (:projectIds)");
				params.addValue("projectIds", scope.getProjectIds());
			}
		}
		if (scope.hasDateRange()) {
		    conditions.add("confirmed_at BETWEEN :fromDate AND :toDate");
		}

		if (!conditions.isEmpty()) {
			sql.append(" WHERE ").append(String.join(" AND ", conditions));
		}
		sql.append(" GROUP BY agent_id, agent_name");
		sql.append(" ORDER BY commission_payable DESC");
		return jdbc.query(sql.toString(), params, new BeanPropertyRowMapper<>(CommissionSummaryDTO.class));
	}

}
