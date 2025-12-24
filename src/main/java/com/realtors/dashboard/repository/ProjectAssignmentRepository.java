package com.realtors.dashboard.repository;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.realtors.dashboard.dto.UserRole;

import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ProjectAssignmentRepository {

    private final NamedParameterJdbcTemplate jdbc;


    public List<UUID> findProjectsByUserAndRole(UUID userId, UserRole role) {

        String sql = """
            SELECT project_id
            FROM project_users
            WHERE user_id = :userId
              AND role = :role
        """;

        Map<String, Object> params = new HashMap<>();
        params.put("userId", userId);
        params.put("role", role.name());

        return jdbc.query(
            sql,
            params,
            (rs, rowNum) -> UUID.fromString(rs.getString("project_id"))
        );
    }
}
