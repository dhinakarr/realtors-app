package com.realtors.admin.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.realtors.admin.dto.AppUserDto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.postgresql.util.PGobject;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Service
public class UserService extends AbstractBaseService<AppUserDto, UUID>{

    private static final Logger logger = LoggerFactory.getLogger(AppUserService.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
    public UserService(JdbcTemplate jdbcTemplate) {
    	super(AppUserDto.class, "app_users", jdbcTemplate); 
    	this.jdbcTemplate = jdbcTemplate;
    }
    
	@Override
	protected String getIdColumn() {
		return "user_id";
	}

    // ✅ RowMapper for AppUserDto
    private AppUserDto mapRow(ResultSet rs, int rowNum) throws SQLException {
        AppUserDto dto = new AppUserDto();
        dto.setUserId((UUID) rs.getObject("user_id"));
        dto.setRoleId((UUID) rs.getObject("role_id"));
        dto.setEmail(rs.getString("email"));
        dto.setMobile(rs.getString("mobile"));
        dto.setFullName(rs.getString("full_name"));
        dto.setStatus(rs.getString("status"));
        dto.setCreatedAt(rs.getTimestamp("created_at"));
        dto.setUpdatedAt(rs.getTimestamp("updated_at"));
        dto.setLastLogin(rs.getTimestamp("last_login"));
        dto.setCreatedBy((UUID) rs.getObject("created_by"));
        dto.setUpdatedBy((UUID) rs.getObject("updated_by"));
        dto.setRoleName(rs.getString("role_name")); // join with role_name

        PGobject metaObj = (PGobject) rs.getObject("meta");
        try {
            if (metaObj != null && metaObj.getValue() != null) {
                dto.setMeta(objectMapper.readValue(metaObj.getValue(), new TypeReference<Map<String, Object>>() {}));
            } else {
                dto.setMeta(new HashMap<>());
            }
        } catch (Exception e) {
            logger.error("Error parsing meta JSON", e);
            dto.setMeta(new HashMap<>());
        }

        return dto;
    }

    /** ✅ Create User */
    public AppUserDto createUser(AppUserDto dto) {
        logger.info("Creating user: {}", dto.getEmail());

        /*
        // Manager validation
        if (dto.getManagerId() != null) {
            String checkManagerSql = "SELECT COUNT(*) FROM app_users WHERE user_id = ?";
            Integer count = jdbcTemplate.queryForObject(checkManagerSql, Integer.class, dto.getManagerId());
            if (count == null || count == 0) {
                throw new IllegalArgumentException("Manager not found");
            }
        }
*/
        // Hash password
        String hashedPassword = passwordEncoder.encode(dto.getPasswordHash());
        dto.setPasswordHash(hashedPassword);
        
        return super.create(dto);
/*
        // Insert user
        String sql = """
                INSERT INTO app_users (role_id, email, mobile, password_hash, full_name,
                                       status, meta, manager_id, created_by, updated_by)
                VALUES (?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?)
                RETURNING user_id
                """;

        PGobject metaObj = new PGobject();
        try {
            metaObj.setType("jsonb");
            metaObj.setValue(objectMapper.writeValueAsString(dto.getMeta() != null ? dto.getMeta() : new HashMap<>()));
        } catch (Exception e) {
            logger.error("Error setting meta JSON", e);
        }

        UUID userId = jdbcTemplate.queryForObject(sql, UUID.class,
                dto.getRoleId(),
                dto.getEmail(),
                dto.getMobile(),
                hashedPassword,
                dto.getFullName(),
                "ACTIVE",
                metaObj,
                dto.getManagerId(),
                currentUserId,
                currentUserId
        );
*/
//        return getUserById(userId).orElseThrow(() -> new IllegalStateException("User creation failed"));
    }

    /** ✅ Update user */
    public AppUserDto updateUser(AppUserDto dto, UUID currentUserId) {
    	return super.update(currentUserId, dto);
    	/*
        String sql = """
                UPDATE app_users
                SET role_id = ?, email = ?, mobile = ?, full_name = ?
                    status = ?, updated_at = CURRENT_TIMESTAMP, updated_by = ?
                WHERE user_id = ? AND status != 'INACTIVE'
                """;

        int updated = jdbcTemplate.update(sql,
                dto.getRoleId(),
                dto.getEmail(),
                dto.getMobile(),
                dto.getFullName(),
                dto.getStatus(),
                currentUserId,
                dto.getUserId()
        );

        return updated > 0 ? getUserById(dto.getUserId()) : Optional.empty();
        */
    }

    /** ✅ Soft delete */
    public boolean softDeleteUser(UUID userId) {
    	return super.softDelete(userId);
    	/*
        String sql = "UPDATE app_users SET status = 'INACTIVE', updated_by = ?, updated_at = CURRENT_TIMESTAMP WHERE user_id = ?";
        int rows = jdbcTemplate.update(sql, currentUserId, userId);
        return rows > 0;
        */
    }

    public AppUserDto partialUpdate(UUID id, Map<String, Object> dto) {
    	return super.patch(id, dto);
//        GenericUpdateUtil.partialUpdate("users", "user_id", id, dto, jdbcTemplate);
    }
    
    /** ✅ Update meta JSONB */
    public boolean updateMeta(UUID id, Map<String, Object> meta) {
        try {
            PGobject metaObj = new PGobject();
            metaObj.setType("jsonb");
            metaObj.setValue(objectMapper.writeValueAsString(meta));
            int rows = jdbcTemplate.update("UPDATE app_users SET meta = ?, updated_at = CURRENT_TIMESTAMP WHERE user_id = ?", metaObj, id);
            return rows > 0;
        } catch (Exception e) {
            logger.error("Error updating meta", e);
            return false;
        }
    }

    /** ✅ Update last login */
    public boolean updateLastLogin(UUID userId) {
        int rows = jdbcTemplate.update("UPDATE app_users SET last_login = CURRENT_TIMESTAMP WHERE user_id = ?", userId);
        return rows > 0;
    }

    /** ✅ Get all active users */
    public List<AppUserDto> getAllUsers() {
    	/*
        String sql = """
                SELECT u.*, r.role_name 
                FROM app_users u
                LEFT JOIN roles r ON u.role_id = r.role_id
                WHERE u.status != 'INACTIVE'
                ORDER BY u.created_at DESC
                """;
        return jdbcTemplate.query(sql, this::mapRow);
        */
    	return super.findAll();
    }

    /** ✅ Get user by ID */
    public Optional<AppUserDto> getUserById(UUID id) {
    	/*
        String sql = """
                SELECT u.*, r.role_name 
                FROM app_users u
                LEFT JOIN roles r ON u.role_id = r.role_id
                WHERE u.user_id = ?
                """;
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, this::mapRow, id));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
        */
    	return super.findById(id);
    }
}

