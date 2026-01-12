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
		
		if (scope.isCustomer()) {
		    conditions.add("customer_id = :customerId");
		    params.addValue("customerId", scope.getCustomerId());
		}

		// Only add conditions if not 'all'
		else if (!scope.isAll()) {
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
		    params.addValue("fromDate", scope.getFromDate());
		    params.addValue("toDate", scope.getToDate());
		}

		if (!conditions.isEmpty()) {
			sql.append(" WHERE ").append(String.join(" AND ", conditions));
		}

		sql.append(" GROUP BY project_id, project_name");
		return jdbcTemplate.query(sql.toString(), params, new BeanPropertyRowMapper<>(FinancialSummaryDTO.class));
	}

}
