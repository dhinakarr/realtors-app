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
	    boolean teamView = scope.isTeamView();
	    StringBuilder sql = new StringBuilder("SELECT ");

	    if (teamView) {
	        sql.append("""
	            team_head_id AS agent_id,
	            team_head_name AS agent_name,
	            COUNT(sale_id) AS total_sales,
	            COALESCE(SUM(sale_amount), 0) AS sales_value,
	        	COALESCE(SUM(area), 0) AS total_area
	        """);
	    } else {
	        sql.append("""
	            agent_id,
	            agent_name,
	            COUNT(sale_id) AS total_sales,
	            COALESCE(SUM(sale_amount), 0) AS sales_value,
	        	COALESCE(SUM(area), 0) AS total_area
	        """);
	    }

	    sql.append(" FROM v_receivable_details ");

	    MapSqlParameterSource params = new MapSqlParameterSource();
	    List<String> conditions = new ArrayList<>();

	    if (scope.isCustomer()) {
	        conditions.add("customer_id = :customerId");
	        params.addValue("customerId", scope.getCustomerId());
	    }

	    if (!scope.isAll() && scope.getUserIds()!=null && !scope.getUserIds().isEmpty()) {
	        conditions.add("agent_id IN (:userIds)");
	        params.addValue("userIds", scope.getUserIds());
	    }

	    if (scope.hasProjectScope() && scope.getProjectIds()!=null && !scope.getProjectIds().isEmpty()) {
	        conditions.add("project_id IN (:projectIds)");
	        params.addValue("projectIds", scope.getProjectIds());
	    }
	    
	    if (scope.hasDateRange()) {
		    conditions.add("sale_date >= :fromDate");
		    conditions.add("sale_date < :toDate");

		    params.addValue("fromDate", scope.getFromDate());
		    params.addValue("toDate", scope.getToDate().plusDays(1));
		}

	    if (!conditions.isEmpty()) {
	        sql.append(" WHERE ").append(String.join(" AND ", conditions));
	    }

	    if (teamView) {
	        sql.append(" GROUP BY team_head_id, team_head_name");
	    } else {
	        sql.append(" GROUP BY agent_id, agent_name");
	    }

	    sql.append(" ORDER BY sales_value DESC");
//logger.info("@DashboardAgentRepository.fetchAgentPerformance sql: "+ sql);
	    return jdbc.query(sql.toString(), params,
	            new BeanPropertyRowMapper<>(AgentPerformanceDTO.class));
	}
}
