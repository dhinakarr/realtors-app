package com.realtors.admin.service;

import com.realtors.admin.dto.AuthResponse;
import com.realtors.admin.dto.LoginResponse;
import com.realtors.admin.dto.ModulePermissionDto;
import com.realtors.alerts.domain.event.EventType;
import com.realtors.alerts.domain.event.ForgotPasswordEvent;
import com.realtors.alerts.dto.RecipientDetail;
import com.realtors.common.EnumConstants;
import com.realtors.common.service.AuditTrailService;
import com.realtors.common.util.JwtUtil;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class UserAuthService {

	@Autowired
	private JdbcTemplate jdbcTemplate;
	private JwtUtil jwtUtil;
	private TokenCacheService tokenServce;
	private AclPermissionService permissionService;
	private final AuditTrailService audit;
	private final ApplicationEventPublisher publisher;
	private static final Logger logger = LoggerFactory.getLogger(UserAuthService.class);

	public UserAuthService(JdbcTemplate jdbcTemplate, JwtUtil jwtUtil, TokenCacheService tokenServce,
			AclPermissionService permissionService, AuditTrailService audit, ApplicationEventPublisher publisher) {
		this.jdbcTemplate = jdbcTemplate;
		this.jwtUtil = jwtUtil;
		this.tokenServce = tokenServce;
		this.permissionService = permissionService;
		this.audit = audit;
		this.publisher = publisher;
	}

	private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	public RecipientDetail getRecipientDetail(UUID userId) {
		String sql = "SELECT  * FROM user_auth where user_id=? ";
		List<Map<String, Object>> data = jdbcTemplate.queryForList(sql, userId);
		if (data.isEmpty())
			return null;

		UUID user = (UUID) data.get(0).get("user_id");
		String email = (String) data.get(0).get("username");
		String mobile = (String) data.get(0).get("mobile");
		return new RecipientDetail(user, email, mobile);
	}

	public LoginResponse login(String email, String password) {
		String authSql = "SELECT  * FROM user_auth where username=? ";
		List<Map<String, Object>> authData = jdbcTemplate.queryForList(authSql, email);
		if (authData.isEmpty())
			throw new IllegalArgumentException("Invalid email or password");

		String hashedPassword = (String) authData.get(0).get("password_hash");
		UUID userId = (UUID) authData.get(0).get("user_id");
		String userType = (String) authData.get(0).get("user_type");
		UUID roleId = (UUID) authData.get(0).get("role_id");
		// Validating Password
		if (!passwordEncoder.matches(password, hashedPassword)) {
			throw new IllegalArgumentException("Invalid username or password");
		}
		// Handling Token
		String token = jwtUtil.generateToken(email, userId.toString(), roleId);
		String refToken = jwtUtil.generateRefreshToken(userId.toString());
		tokenServce.storeTokens(userId.toString(), token, refToken);
		Boolean forcePasswordChange = (Boolean) authData.get(0).get("force_password_change");

		// Update lastLogin data
		updateLastLogin(userId);

		// Build return Value after successful login
		List<ModulePermissionDto> returnDto = this.permissionService.findPermissionsByRole(roleId);
		Map<String, Object> map = Map.of("accessToken", token, "refreshToken", refToken, "userId", userId.toString(),
				"email", email, "roleId", roleId.toString(), "userType", userType, "forcePasswordChange",
				forcePasswordChange);
		audit.auditAsync("user_auth", userId, EnumConstants.LOGIN);
		return new LoginResponse(map, returnDto);
	}

	public void changePassword(UUID userId, String oldPassword, String newPassword) {
		String sql = "SELECT password_hash FROM user_auth WHERE user_id=?";
		List<Map<String, Object>> data = jdbcTemplate.queryForList(sql, userId);

		if (data.isEmpty()) {
			throw new IllegalArgumentException("User not found");
		}
		String currentHash = (String) data.get(0).get("password_hash");

		if (!passwordEncoder.matches(oldPassword, currentHash)) {
			throw new IllegalArgumentException("Old password is incorrect");
		}
		String newHash = passwordEncoder.encode(newPassword);
		jdbcTemplate.update(
				"UPDATE user_auth SET password_hash=?, force_password_change=false, failed_attempts=0 WHERE user_id=?",
				newHash, userId);

		audit.auditAsync("user_auth", userId, EnumConstants.UPDATE);
	}

	public void generateResetToken(String email) {
		String sql = "SELECT user_id FROM user_auth WHERE username=?";
		List<Map<String, Object>> data = jdbcTemplate.queryForList(sql, email);

		if (data.isEmpty()) {
			return;
		}
		Object userIdObj = data.get(0).get("user_id");
		UUID userId = userIdObj instanceof UUID ? (UUID) userIdObj : UUID.fromString(userIdObj.toString());
		jdbcTemplate.update("DELETE FROM password_reset_token WHERE user_id=?", userId);

		String checkSql = """
				    SELECT COUNT(*) FROM password_reset_token
				    WHERE user_id = ?
				    AND created_at > NOW() - INTERVAL '1 minute'
				""";

		Integer recentCount = jdbcTemplate.queryForObject(checkSql, Integer.class, userId);

		if (recentCount != null && recentCount > 0) {
			logger.warn("Too many reset requests for user {}", userId);
			return;
		}
		String token = UUID.randomUUID().toString();
		String hashedToken = DigestUtils.sha256Hex(token);
		jdbcTemplate.update("INSERT INTO password_reset_token (user_id, token_hash, expiry_time) VALUES (?, ?, ?)",
				userId, hashedToken, Timestamp.valueOf(LocalDateTime.now().plusMinutes(15)));

		// send email
		publisher.publishEvent(new ForgotPasswordEvent(userId.toString(), EventType.FORGOT_PASSWORD.name(), email,
				email, token, userId));
	}

	public AuthResponse resetPassword(String token, String newPassword) {
		String sql = "SELECT * FROM password_reset_token WHERE token_hash=?";
		String hashedToken = DigestUtils.sha256Hex(token);

		List<Map<String, Object>> data = jdbcTemplate.queryForList(sql, hashedToken);

		if (data.isEmpty()) {
			throw new IllegalArgumentException("Invalid token");
		}
		Map<String, Object> tokenData = data.get(0);
		boolean used = (Boolean) tokenData.get("used");
		Timestamp expiry = (Timestamp) tokenData.get("expiry_time");

		if (used || expiry.toLocalDateTime().isBefore(LocalDateTime.now())) {
			throw new IllegalArgumentException("Token expired or already used");
		}

		Object userIdObj = tokenData.get("user_id");
		UUID userId = userIdObj instanceof UUID ? (UUID) userIdObj : UUID.fromString(userIdObj.toString());
		String newHash = passwordEncoder.encode(newPassword);
		jdbcTemplate.update("UPDATE user_auth SET password_hash=?, force_password_change=false WHERE user_id=?",
				newHash, userId);
		jdbcTemplate.update("UPDATE password_reset_token SET used=true WHERE token_hash=?", hashedToken);

		String userSql = "select * from user_auth where user_id=?";
		List<Map<String, Object>> userObj = jdbcTemplate.queryForList(userSql, userId);
		Map<String, Object> row = userObj.stream().findFirst()
				.orElseThrow(() -> new RuntimeException("User not found"));
		String email = (String) row.get("username");
		UUID roleId = (UUID) row.get("role_id");
		String accessToken = jwtUtil.generateToken(email, userId.toString(), roleId);
		String refreshToken = jwtUtil.generateRefreshToken(userId.toString());

		audit.auditAsync("user_auth", userId, EnumConstants.UPDATE);
		return new AuthResponse(userId, accessToken, refreshToken);
	}

	private boolean updateLastLogin(UUID userId) {
		int rows = jdbcTemplate.update("UPDATE user_auth SET last_login = CURRENT_TIMESTAMP WHERE user_id = ?", userId);
		audit.auditAsync("user_auth", userId, EnumConstants.UPDATE);
		return rows > 0;
	}

	public void createUserAuth(UUID userId, String username, String password, UUID roleId, String mobile,
			String userType) {
		String hashedPassword = passwordEncoder.encode(password == null ? "Test@123" : password);
		String type = Optional.ofNullable(userType)
                .filter(s -> !s.trim().isEmpty())
                .orElse("INTERNAL");
		String insertSql = "INSERT INTO user_auth (user_id, username, password_hash, role_id, mobile, user_type) values(?,?,?,?,?, ?)";
		audit.auditAsync("user_auth", userId, EnumConstants.CREATE);
		jdbcTemplate.update(insertSql, new PreparedStatementSetter() {
			@Override
			public void setValues(PreparedStatement ps) throws SQLException {
				ps.setObject(1, userId);
				ps.setString(2, username);
				ps.setString(3, hashedPassword);
				ps.setObject(4, roleId);
				ps.setString(5, mobile);
				ps.setString(6, type);
			}
		});
	}

	public UUID createOrGetUser(UUID userId, String username, String password, UUID roleId, String mobile,
			String userType) {

		String sql = "SELECT user_id FROM user_auth WHERE username = ?";
		List<UUID> existing = jdbcTemplate.queryForList(sql, UUID.class, username);

		if (!existing.isEmpty()) {
			return existing.get(0); // ✅ reuse existing
		}

		String hashedPassword = passwordEncoder.encode(password == null ? "Test@123" : password);
		String type = userType == null ? "INTERNAL" : "CUSTOMER";

		jdbcTemplate.update("""
				INSERT INTO user_auth (user_id, username, password_hash, role_id, mobile, user_type)
				VALUES (?, ?, ?, ?, ?, ?)
				""", userId, username, hashedPassword, roleId, mobile, type);

		return userId;
	}

	public boolean emailExists(String username, String mobile) {
		StringBuilder sql = new StringBuilder("""
				    SELECT COUNT(*)
				    FROM user_auth
				""");

		List<String> conditions = new ArrayList<>();
		List<Object> params = new ArrayList<>();

		if (username != null && !username.isBlank()) {
			conditions.add("username = ?");
			params.add(username);
		}
		if (mobile != null && !mobile.isBlank()) {
			conditions.add("mobile = ?");
			params.add(mobile);
		}
		if (conditions.isEmpty()) {
			return false;
		}

		sql.append(" WHERE ");
		sql.append(String.join(" OR ", conditions));
		Integer count = jdbcTemplate.queryForObject(sql.toString(), params.toArray(), Integer.class);

		return count != null && count > 0;
	}

	public boolean isUserPresent(UUID userId) {
		String authSql = "SELECT  * FROM user_auth where user_id=? ";
		List<Map<String, Object>> authData = jdbcTemplate.queryForList(authSql, userId);
		if (authData.isEmpty())
			return false;
		else
			return true;
	}
}
