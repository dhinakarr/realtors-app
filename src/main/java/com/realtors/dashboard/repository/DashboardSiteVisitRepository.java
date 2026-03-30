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
import com.realtors.dashboard.dto.SiteVisitDetailsDTO;
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
				         COUNT(DISTINCT CASE
				    WHEN s.sale_id IS NOT NULL THEN svc.customer_id
				END) AS conversions,
				         COUNT(DISTINCT CASE
				    WHEN s.sale_id IS NOT NULL THEN svc.customer_id
				END)
				/ COUNT(DISTINCT svc.customer_id) AS conversion_ratio
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
		if (scope.isCustomer()) {
			conditions.add("svc.customer_id = :customerId");
			params.addValue("customerId", scope.getCustomerId());
		} else if (!scope.isAll()) {

			// WHOSE data
			if (scope.getUserIds() != null && !scope.getUserIds().isEmpty()) {
				conditions.add("sv.user_id IN (:userIds)");
				params.addValue("userIds", scope.getUserIds());
			}

			// WHICH projects
			if (scope.hasProjectScope() && scope.getProjectIds() != null && !scope.getProjectIds().isEmpty()) {

				conditions.add("sv.project_id IN (:projectIds)");
				params.addValue("projectIds", scope.getProjectIds());
			}
		}
		if (scope.hasDateRange()) {
			conditions.add("sv.visit_date >= :fromDate");
			conditions.add("sv.visit_date < :toDate");

			params.addValue("fromDate", scope.getFromDate());
			params.addValue("toDate", scope.getToDate().plusDays(1));
		}

		if (!conditions.isEmpty()) {
			sql.append(" WHERE ").append(String.join(" AND ", conditions));
		}

		sql.append("""
				    GROUP BY sv.user_id, u.full_name
				    ORDER BY conversion_ratio DESC NULLS LAST
				""");

		return jdbc.query(sql.toString(), params, new BeanPropertyRowMapper<>(SiteVisitSummaryDTO.class));
	}

	public List<SiteVisitDetailsDTO> fetchSiteVisitDetails(DashboardScope scope) {

		StringBuilder sql = new StringBuilder(
				"""
						    SELECT sv.site_visit_id, sv.visit_date, u.user_id  AS agent_id, u.full_name  AS agent_name, p.project_id, p.project_name, c.customer_id,
						        c.customer_name,
						        CASE
						            WHEN s.sale_id IS NOT NULL THEN TRUE
						            ELSE FALSE
						        END AS is_converted,

						        s.sale_id, s.total_price AS sale_price, s.confirmed_at
						    FROM site_visits sv
						    JOIN app_users u
						        ON u.user_id = sv.user_id
						    LEFT JOIN site_visit_customers svc
						        ON svc.site_visit_id = sv.site_visit_id
						    LEFT JOIN customers c
						        ON c.customer_id = svc.customer_id
						    LEFT JOIN projects p
						        ON p.project_id = sv.project_id
						    LEFT JOIN sales s
						        ON s.customer_id = svc.customer_id
						       AND s.project_id = sv.project_id
						       AND s.sold_by = sv.user_id
						""");

		MapSqlParameterSource params = new MapSqlParameterSource();
		List<String> conditions = new ArrayList<>();
		if (scope.isCustomer()) {
			conditions.add("svc.customer_id = :customerId");
			params.addValue("customerId", scope.getCustomerId());
		} else if (!scope.isAll()) {

			// WHOSE data
			if (scope.getUserIds() != null && !scope.getUserIds().isEmpty()) {
				conditions.add("sv.user_id IN (:userIds)");
				params.addValue("userIds", scope.getUserIds());
			}

			// WHICH projects
			if (scope.hasProjectScope() && scope.getProjectIds() != null && !scope.getProjectIds().isEmpty()) {

				conditions.add("sv.project_id IN (:projectIds)");
				params.addValue("projectIds", scope.getProjectIds());
			}
		}
		if (scope.hasDateRange()) {
			conditions.add("sv.visit_date >= :fromDate");
			conditions.add("sv.visit_date < :toDate");

			params.addValue("fromDate", scope.getFromDate());
			params.addValue("toDate", scope.getToDate().plusDays(1));
		}

		if (!conditions.isEmpty()) {
			sql.append(" WHERE ").append(String.join(" AND ", conditions));
		}
		sql.append("""
				    ORDER BY sv.visit_date DESC, c.customer_name
				""");
		return jdbc.query(sql.toString(), params, new BeanPropertyRowMapper<>(SiteVisitDetailsDTO.class));
	}
}
