package com.realtors.admin.service;

import com.realtors.admin.dto.LoginResponse;
import com.realtors.admin.dto.ModulePermissionDto;
import com.realtors.common.EnumConstants;
import com.realtors.common.service.AuditContext;
import com.realtors.common.service.AuditTrailService;
import com.realtors.common.util.AppUtil;
import com.realtors.common.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.sql.PreparedStatement;
import java.sql.SQLException;
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

	public UserAuthService(JdbcTemplate jdbcTemplate, JwtUtil jwtUtil, 
			TokenCacheService tokenServce, AclPermissionService permissionService, AuditTrailService audit) {
		this.jdbcTemplate = jdbcTemplate;
		this.jwtUtil = jwtUtil;
		this.tokenServce = tokenServce;
		this.permissionService = permissionService;
		this.audit = audit;
	}

	private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	public LoginResponse login(String email, String password) {
		String authSql = "SELECT  * FROM user_auth where username=? ";
		List<Map<String, Object>> authData = jdbcTemplate.queryForList(authSql, email);
		if (authData == null)
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

		// Update lastLogin data
		updateLastLogin(userId);

		// Build return Value after successful login
		List<ModulePermissionDto> returnDto = this.permissionService.findPermissionsByRole(roleId);
		Map<String, Object> map = Map.of("accessToken", token, "refreshToken", refToken, "userId", userId.toString(),
				"email", email, "roleId",roleId.toString(), "userType", userType);
		audit.auditAsync("AUTH", userId, "LOGIN", AppUtil.getCurrentUserId(), AuditContext.getIpAddress(),
				AuditContext.getUserAgent());
		return new LoginResponse(map, returnDto);
	}
	
	 private boolean updateLastLogin(UUID userId) {
	        int rows = jdbcTemplate.update("UPDATE user_auth SET last_login = CURRENT_TIMESTAMP WHERE user_id = ?", userId);
	        audit.auditAsync("users", userId, EnumConstants.UPDATE.toString(), 
	    			AppUtil.getCurrentUserId(), AuditContext.getIpAddress(), AuditContext.getUserAgent());
	        return rows > 0;
	 }
	 
	 public void createUserAuth(UUID userId, String username, String password, UUID roleId) {
		 createUserAuth(userId, username, password, roleId, null);
	 }
	 
	 public void createUserAuth(UUID userId, String username, String password, UUID roleId, String userType) {
		 String hashedPassword = passwordEncoder.encode(password==null? "Test@123":password);
		 String type = userType == null ? "INTERNAL" : "CUSTOMER";
		 String insertSql= "INSERT INTO user_auth (user_id, username, password_hash, role_id, user_type) values(?,?,?,?,?)";
		 jdbcTemplate.update(insertSql, new PreparedStatementSetter() {
	            @Override
	            public void setValues(PreparedStatement ps) throws SQLException {
	                ps.setObject(1, userId);
	                ps.setString(2, username);
	                ps.setString(3, hashedPassword);
	                ps.setObject(4, roleId);
	                ps.setString(5, type);
	            }
	        });
	 }
}
