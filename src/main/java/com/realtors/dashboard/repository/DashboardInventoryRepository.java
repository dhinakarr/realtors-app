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
import com.realtors.dashboard.dto.InventoryDetailDTO;
import com.realtors.dashboard.dto.InventoryStatusDTO;
import com.realtors.dashboard.dto.InventorySummaryDTO;

@Repository
public class DashboardInventoryRepository {

//    private JdbcTemplate jdbcTemplate;
	private NamedParameterJdbcTemplate jdbcTemplate;
	private static final Logger logger = LoggerFactory.getLogger(DashboardInventoryRepository.class);

	public DashboardInventoryRepository(NamedParameterJdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public List<InventorySummaryDTO> fetchInventorySummary(DashboardScope scope) {
		StringBuilder sql = new StringBuilder("""
				    SELECT project_id, project_name,
				           COUNT(*) AS total_plots,
				           COUNT(*) FILTER (WHERE inventory_status = 'AVAILABLE') AS available,
				           COUNT(*) FILTER (WHERE inventory_status in ( 'BOOKED', 'IN_PROGRESS')) AS booked,
							COUNT(*) FILTER (WHERE inventory_status in ('SOLD', 'COMPLETED')) AS sold,
							COUNT(*) AS total_plots
				    FROM v_inventory_status
				""");

		MapSqlParameterSource params = new MapSqlParameterSource();
		applyProjectScope(scope, sql, params, false);

		sql.append(" GROUP BY project_id, project_name");
		sql.append(" ORDER BY project_name");

		return jdbcTemplate.query(sql.toString(), params, new BeanPropertyRowMapper<>(InventorySummaryDTO.class));
	}

	public List<InventoryStatusDTO> fetchInventoryDetails(DashboardScope scope) {

		StringBuilder sql = new StringBuilder("""
				    SELECT project_id, project_name,  plot_id, plot_number,
				        inventory_status,  area,  base_price, total_price
				    FROM v_inventory_status
				    WHERE 1=1
				""");

		MapSqlParameterSource params = new MapSqlParameterSource();
		applyProjectScope(scope, sql, params, false);
		sql.append(" ORDER BY project_name, plot_number");

		return jdbcTemplate.query(sql.toString(), params, new BeanPropertyRowMapper<>(InventoryStatusDTO.class));
	}

	private void applyProjectScope(DashboardScope scope, StringBuilder sql, MapSqlParameterSource params,
			boolean hasWhereClause) {
		if (scope.isAll()) {
			return;
		}

		if (scope.hasProjectScope() && scope.getProjectIds() != null && !scope.getProjectIds().isEmpty()) {
			sql.append(hasWhereClause ? " AND " : " WHERE ");
			sql.append("project_id IN (:projectIds)");
			params.addValue("projectIds", scope.getProjectIds());
		}
	}

}
