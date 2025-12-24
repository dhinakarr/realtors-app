package com.realtors.dashboard.repository;

import java.util.UUID;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

@Repository
public class DashboardScopeRepository {

	private final NamedParameterJdbcTemplate jdbc;
	
	public DashboardScopeRepository(NamedParameterJdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	public Set<UUID> findProjectsForUser(UUID userId) {
		String sql = """
				    SELECT DISTINCT project_id
				    FROM v_user_project_scope
				    WHERE user_id = :userId
				""";
		return new HashSet<>(jdbc.queryForList(sql, Map.of("userId", userId), UUID.class));
	}
}
