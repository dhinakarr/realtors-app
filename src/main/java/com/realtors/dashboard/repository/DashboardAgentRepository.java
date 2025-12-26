package com.realtors.dashboard.repository;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.realtors.dashboard.dto.AgentPerformanceDTO;
import com.realtors.dashboard.dto.DashboardScope;

@Repository
public class DashboardAgentRepository {

	private final NamedParameterJdbcTemplate jdbc;
	private static final Logger logger = LoggerFactory.getLogger(DashboardAgentRepository.class);

	public DashboardAgentRepository(NamedParameterJdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	public List<AgentPerformanceDTO> fetchAgentPerformance(DashboardScope scope) {
		StringBuilder sql = new StringBuilder("""
				    SELECT agent_id,
				           agent_name,
				           COUNT(sale_id)   AS total_sales,
				           SUM(sale_amount) AS sales_value
				    FROM v_receivable_details
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

		if (!conditions.isEmpty()) {
			sql.append(" WHERE ").append(String.join(" OR ", conditions));
		}
		sql.append(" GROUP BY agent_id, agent_name");
		sql.append(" ORDER BY sales_value DESC");
		return jdbc.query(sql.toString(), params, new BeanPropertyRowMapper<>(AgentPerformanceDTO.class));
	}

}
