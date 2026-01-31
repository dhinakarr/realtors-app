package com.realtors.admin.service;


import com.realtors.admin.dto.LoginResponse;
import com.realtors.admin.dto.ModulePermissionDto;
import com.realtors.common.EnumConstants;
import com.realtors.common.service.AuditTrailService;
import com.realtors.common.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.List;
import java.util.UUID;

@Service
public class AuthService {

    @Autowired
    private JdbcTemplate jdbcTemplate;
    private JwtUtil jwtUtil;
    private UserService userService;
    private TokenCacheService tokenServce;
    private AclPermissionService permissionService;
    private final AuditTrailService audit;
    
    public AuthService(JdbcTemplate jdbcTemplate, JwtUtil jwtUtil, UserService userService, 
    		TokenCacheService tokenServce, AclPermissionService permissionService, AuditTrailService audit) {
    	this.jdbcTemplate = jdbcTemplate;
    	this.jwtUtil = jwtUtil;
    	this.userService = userService;
    	this.tokenServce = tokenServce;
    	this.permissionService = permissionService;
    	this.audit = audit;
    }

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
    public LoginResponse login(String email, String password) {
        String sql = "SELECT user_id, role_id, password_hash FROM app_users WHERE email = ? AND status = 'ACTIVE'";
        List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, email);
        if (results.isEmpty()) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        Map<String, Object> userRow = results.get(0);
        String hashedPassword = (String) userRow.get("password_hash");
        UUID userId = (UUID) userRow.get("user_id");
        UUID roleId = (UUID) userRow.get("role_id");

        // Validating Password
        if (!passwordEncoder.matches(password, hashedPassword)) {
            throw new IllegalArgumentException("Invalid email or password");
        }
       
        // Handling Token
        String token = jwtUtil.generateToken(email, userId.toString(), roleId);
        String refToken = jwtUtil.generateRefreshToken(userId.toString());
        tokenServce.storeTokens(userId.toString(), token, refToken);
        
        // Update lastLogin data 
        this.userService.updateLastLogin(userId);
        
        //Build return Value after successful login
        List<ModulePermissionDto> returnDto = this.permissionService.findPermissionsByRole(roleId);
        Map<String, Object> map = Map.of(
                "accessToken", token,
                "refreshToken", refToken,
                "userId", userId.toString(),
                "email", email
        );
        audit.auditAsync("AUTH", userId, EnumConstants.LOGIN);
        return 	new LoginResponse(map, returnDto);
    }
}

