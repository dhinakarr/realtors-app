package com.realtors.dashboard.repository;

import java.util.List;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.realtors.dashboard.dto.DashboardScope;
import com.realtors.dashboard.dto.SiteVisitSummaryDTO;

@Repository
public class DashboardSiteVisitRepository {

	private final NamedParameterJdbcTemplate jdbc;
	private static final Logger logger = LoggerFactory.getLogger(DashboardSiteVisitRepository.class);

	public DashboardSiteVisitRepository(NamedParameterJdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	public List<SiteVisitSummaryDTO> fetchSiteVisitSummary(DashboardScope scope) {
		StringBuilder sql = new StringBuilder("""
				    SELECT
				        sv.user_id          AS agent_id,
				        u.full_name         AS agent_name,
				        COUNT(DISTINCT sv.site_visit_id) AS total_visits,
				        COUNT(DISTINCT svc.customer_id)  AS total_customers,
				        COUNT(DISTINCT s.sale_id)        AS conversions,
				        ROUND(
				            COUNT(DISTINCT s.sale_id)::numeric
				            / NULLIF(COUNT(DISTINCT sv.site_visit_id), 0) * 100,
				            2
				        ) AS conversion_ratio
				    FROM site_visits sv
				    JOIN app_users u ON u.user_id = sv.user_id
				    LEFT JOIN site_visit_customers svc
				           ON svc.site_visit_id = sv.site_visit_id
				    LEFT JOIN sales s
				           ON s.sold_by = sv.user_id
				          AND s.project_id = sv.project_id
				""");

		MapSqlParameterSource params = new MapSqlParameterSource();
		List<String> conditions = new ArrayList<>();

		if (!scope.isAll()) {
			if (scope.getUserId() != null) {
				conditions.add("sv.user_id = :userId");
				params.addValue("userId", scope.getUserId());
			}
			if (scope.hasProjectScope() && scope.getProjectIds() != null && !scope.getProjectIds().isEmpty()) {
				conditions.add("sv.project_id IN (:projectIds)");
				params.addValue("projectIds", scope.getProjectIds());
			}
		}

		if (!conditions.isEmpty()) {
			sql.append(" WHERE ").append(String.join(" OR ", conditions));
		}

		sql.append("""
				    GROUP BY sv.user_id, u.full_name
				    ORDER BY conversion_ratio DESC NULLS LAST
				""");
		return jdbc.query(sql.toString(), params, new BeanPropertyRowMapper<>(SiteVisitSummaryDTO.class));
	}

}
