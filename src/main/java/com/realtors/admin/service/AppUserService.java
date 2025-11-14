package com.realtors.admin.service;

import com.realtors.admin.dto.AppUserDto;

import org.postgresql.util.PGobject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Service
public class AppUserService {

    private static final Logger logger = LoggerFactory.getLogger(AppUserService.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // RowMapper for AppUserDto
    private AppUserDto mapRow(ResultSet rs, int rowNum) throws SQLException {
        AppUserDto dto = new AppUserDto();
        dto.setUserId((UUID) rs.getObject("user_id"));
        dto.setRoleId((UUID) rs.getObject("role_id"));
        dto.setRoleName(rs.getString("role_name"));
        dto.setEmail(rs.getString("email"));
        dto.setMobile(rs.getString("mobile"));
        dto.setFullName(rs.getString("full_name"));
        dto.setStatus(rs.getString("status"));
        dto.setLastLogin(rs.getTimestamp("last_login"));
        dto.setCreatedAt(rs.getTimestamp("created_at"));
        dto.setUpdatedAt(rs.getTimestamp("updated_at"));
        dto.setManagerId((UUID) rs.getObject("manager_id"));

        PGobject pgObject = (PGobject) rs.getObject("meta");
        try {
            if (pgObject != null && pgObject.getValue() != null) {
                dto.setMeta(objectMapper.readValue(
                    pgObject.getValue(),
                    new TypeReference<Map<String, Object>>() {}
                ));
            } else {
                dto.setMeta(new HashMap<>());
            }
        } catch (Exception e) {
            logger.error("Failed to parse JSON meta", e);
            dto.setMeta(new HashMap<>());
        }

        return dto;
    }

    /** ✅ Create user with password hashing and manager validation */
    public AppUserDto createUser(AppUserDto dto) {
        logger.info("Creating new user with email: {}", dto.getEmail());

        // Validate manager
        if (dto.getManagerId() != null) {
            boolean managerExists = managerExists(dto.getManagerId());
            if (!managerExists) {
                throw new IllegalArgumentException("Manager not found for ID: " + dto.getManagerId());
            }
        }
        // Hash password
        String hashedPassword = passwordEncoder.encode(dto.getPasswordHash());

        String sql = """
            INSERT INTO app_users (role_id, email, mobile, password_hash, full_name, 
                                   manager_id, created_by, status)
            VALUES (?, ?, ?, ?, ?, ?, ?, 'ACTIVE')
            RETURNING user_id
        """;

        UUID userId = jdbcTemplate.queryForObject(sql, UUID.class,
                dto.getRoleId(),
                dto.getEmail(),
                dto.getMobile(),
                hashedPassword,
                dto.getFullName(),
                dto.getManagerId(),
                dto.getCreatedBy()
        );

        return getUserById(userId).orElseThrow(() -> new RuntimeException("User creation failed"));
    }

    /** ✅ Get all active users */
    public List<AppUserDto> getAllUsers() {
        logger.info("Fetching all active users");
        String sql = """
            SELECT u.*, r.role_name
            FROM app_users u
            JOIN roles r ON u.role_id = r.role_id
            WHERE u.status = 'ACTIVE'
            ORDER BY u.created_at DESC
        """;
        return jdbcTemplate.query(sql, this::mapRow);
    }

    /** ✅ Get single user */
    public Optional<AppUserDto> getUserById(UUID userId) {
        logger.info("Fetching user by ID: {}", userId);
        try {
            String sql = """
                SELECT u.*, r.role_name
                FROM app_users u
                JOIN roles r ON u.role_id = r.role_id
                WHERE u.user_id = ?
            """;
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, this::mapRow, userId));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /** ✅ Update user (soft updates, excluding password/meta) */
    public Optional<AppUserDto> updateUser(AppUserDto dto) {
        logger.info("Updating user with ID: {}", dto.getUserId());
        String sql = """
            UPDATE app_users
            SET role_id = ?, email = ?, mobile = ?, full_name = ?,
                manager_id = ?, updated_by = ?, updated_at = CURRENT_TIMESTAMP, status = ?
            WHERE user_id = ?
        """;
        int updated = jdbcTemplate.update(sql,
                dto.getRoleId(),
                dto.getEmail(),
                dto.getMobile(),
                dto.getFullName(),
                dto.getManagerId(),
                dto.getUpdatedBy(),
                dto.getStatus(),
                dto.getUserId()
        );
        return updated > 0 ? getUserById(dto.getUserId()) : Optional.empty();
    }

    /** ✅ Soft delete user */
    public boolean softDeleteUser(UUID userId, UUID updatedBy) {
        logger.warn("Soft deleting user ID: {}", userId);
        String sql = "UPDATE app_users SET status = 'INACTIVE', updated_by = ?, updated_at = CURRENT_TIMESTAMP WHERE user_id = ?";
        return jdbcTemplate.update(sql, updatedBy, userId) > 0;
    }

    /** ✅ Update last login timestamp */
    public boolean updateLastLogin(UUID userId) {
        logger.info("Updating last login for user ID: {}", userId);
        String sql = "UPDATE app_users SET last_login = CURRENT_TIMESTAMP WHERE user_id = ?";
        return jdbcTemplate.update(sql, userId) > 0;
    }

    /** ✅ Update meta JSONB field */
    public boolean updateMeta(UUID userId, Map<String, Object> meta) {
        logger.info("Updating meta data for user ID: {}", userId);
        String sql = "UPDATE app_users SET meta = ?::jsonb, updated_at = CURRENT_TIMESTAMP WHERE user_id = ?";
        return jdbcTemplate.update(sql, meta.toString(), userId) > 0;
    }

    /** ✅ Manager validation */
    private boolean managerExists(UUID managerId) {
        String sql = "SELECT COUNT(*) FROM app_users WHERE user_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, managerId);
        return count != null && count > 0;
    }
}
