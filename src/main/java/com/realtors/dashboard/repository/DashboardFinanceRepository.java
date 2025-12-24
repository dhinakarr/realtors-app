package com.realtors.dashboard.repository;

import java.util.List;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.realtors.dashboard.dto.FinancialSummaryDTO;
import com.realtors.dashboard.dto.DashboardScope;


@Repository
public class DashboardFinanceRepository {

//    private JdbcTemplate jdbcTemplate;
	private NamedParameterJdbcTemplate jdbcTemplate;
	private static final Logger logger = LoggerFactory.getLogger(DashboardFinanceRepository.class);

	public DashboardFinanceRepository(NamedParameterJdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public List<FinancialSummaryDTO> fetchFinancialSummary(DashboardScope scope) {

		StringBuilder sql = new StringBuilder("""
				    SELECT project_id, project_name,
				           SUM(sale_amount) AS total_sales,
				           SUM(total_received) AS total_received,
				           SUM(outstanding_amount) AS total_outstanding
				    FROM v_receivable_details
				""");

		MapSqlParameterSource params = new MapSqlParameterSource();
		List<String> conditions = new ArrayList<>();

		// Only add conditions if not 'all'
		if (!scope.isAll()) {
			if (scope.getUserId() != null) {
				conditions.add("agent_id = :userId");
				params.addValue("userId", scope.getUserId());
			}
			if (scope.hasProjectScope() && scope.getProjectIds() != null && !scope.getProjectIds().isEmpty()) {
				conditions.add("project_id IN (:projectIds)");
				params.addValue("projectIds", scope.getProjectIds());
			}
		}

		if (!conditions.isEmpty()) {
			sql.append(" WHERE ").append(String.join(" OR ", conditions));
		}

		sql.append(" GROUP BY project_id, project_name");
		return jdbcTemplate.query(sql.toString(), params, new BeanPropertyRowMapper<>(FinancialSummaryDTO.class));
	}

}
