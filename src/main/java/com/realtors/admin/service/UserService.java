package com.realtors.admin.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.realtors.admin.dto.AppUserDto;
import com.realtors.admin.dto.PagedResult;
import com.realtors.admin.dto.form.DynamicFormResponseDto;
import com.realtors.admin.dto.form.EditResponseDto;
import com.realtors.admin.dto.form.LookupDefinition;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.postgresql.util.PGobject;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Service
public class UserService extends AbstractBaseService<AppUserDto, UUID>{

    private static final Logger logger = LoggerFactory.getLogger(AppUserService.class);
    ObjectMapper mapper = new ObjectMapper();

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
    List<LookupDefinition> lookupDefs = List.of(
    	    new LookupDefinition("roles", "roles", "role_id", "role_name", "roleName"),
    	    new LookupDefinition("app_users", "app_users", "manager_id", "full_name", "managerName")
    	);
    
    public UserService(JdbcTemplate jdbcTemplate) {
    	super(AppUserDto.class, "app_users", jdbcTemplate, Set.of("role_name", "managerName")); 
    	// Add multiple foreign key lookups
        addDependentLookup("role_id", "roles", "role_id", "role_name", "roleName");
        addDependentLookup("manager_id", "app_users", "user_id", "full_name", "managerName");
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
//        dto.setRoleName(rs.getString("role_name")); // join with role_name

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
    
    /** ✅ User form response */
    public DynamicFormResponseDto getUserFormData() {
    	return super.buildDynamicFormResponse();
    }
    
    /** ✅ Update user form response */
    public EditResponseDto<AppUserDto> editUserResponse(UUID currentUserId) {
        Optional<AppUserDto> opt = super.findById(currentUserId);
        DynamicFormResponseDto form = super.buildDynamicFormResponse();
        
        return opt.map(user -> new EditResponseDto<>(user, form))
                  .orElse(null);
    }

    /** ✅ Create User */
    public AppUserDto createUser(AppUserDto dto) {
        logger.info("@UserService.createUser Creating user: {} {}", dto.toString());

        // Manager validation
        if (dto.getManagerId() != null) {
            String checkManagerSql = "SELECT COUNT(*) FROM app_users WHERE user_id = ?";
            Integer count = jdbcTemplate.queryForObject(checkManagerSql, Integer.class, dto.getManagerId());
            if (count == null || count == 0) {
                throw new IllegalArgumentException("@UserService.createUser Manager not found");
            }
        } else {
        	throw new IllegalArgumentException("@UserService.createUser User must have assigned with Manager ");
        }
        // Hash password
        String hashedPassword = passwordEncoder.encode(dto.getPasswordHash()==null? "Test@123":dto.getPasswordHash());
        dto.setPasswordHash(hashedPassword);
        
        return super.create(dto);
    }

    /** ✅ Update user */
    public AppUserDto updateUser(AppUserDto dto, UUID currentUserId) {
    	return super.update(currentUserId, dto);
    }
    
 // ---------------- CREATE ----------------
    public AppUserDto createWithFiles(AppUserDto data, MultipartFile profileImage) {
    	// Hash password
        String hashedPassword = passwordEncoder.encode(data.getPasswordHash()==null? "Test@123":data.getPasswordHash());
        data.setPasswordHash(hashedPassword);
        
    	byte[] imageBytes = null;
    	try {
            if (profileImage != null && !profileImage.isEmpty()) {
                imageBytes = profileImage.getBytes();
            }
            data.setProfileImage(imageBytes);
        } catch (IOException ioe) {
            logger.error("Error reading profile image", ioe);
            return null;
        }

        // meta is ALREADY a Map<String, Object> → no need to parse!
        Map<String, Object> meta = data.getMeta();  // ← Just get it directly

        // If you want a mutable copy (safe)
        Map<String, Object> metaMap = (meta != null) 
            ? new HashMap<>(meta)  // copy it
            : new HashMap<>();     // or empty map
        data.setMeta(metaMap);
    	Map<String, Object> updatedMap = mapper.convertValue(data, Map.class);
        // You can use a GenericInsertUtil that supports files
        return super.createWithFiles( updatedMap);
    }

    // ---------------- UPDATE ----------------
    public AppUserDto updateWithFiles(UUID id, Map<String, Object> updates) {
        return super.patchUpdateWithFile(id, updates);
    }
    
    /** ✅ Soft delete */
    public boolean softDeleteUser(UUID userId) {
    	return super.softDelete(userId);
    }

    public AppUserDto partialUpdate(UUID id, Map<String, Object> dto) {
    	return super.patch(id, dto);
    }
    
 // Search Use data
    public List<AppUserDto> searchUsers(String searchText) {
    	return super.search(searchText, List.of("full_name", "email"), null);
    }
    
    // Get Paged modules data thi
    public PagedResult<AppUserDto> getPaginatedUsers(int page, int size) {
    	
		/*
		 * return new FeatureListResponseDto<>( "Users", "table", List.of("Full Name",
		 * "Email", "Mobile", "Role Name", "Status"), Map.ofEntries(
		 * Map.entry("Full Name", "fullName"), Map.entry("Email", "email"),
		 * Map.entry("Mobile", "mobile"), Map.entry("Role Name", "roleName"),
		 * Map.entry("Status", "status") ), "userId", true, // pagination enabled
		 * super.findAllPaginated(page, size, null), // <-- MUST return
		 * PagedResult<AppUserDto> super.getLookupData(lookupDefs) // <-- fully dynamic
		 * lookup map );
		 */
    	return super.findAllPaginated(page, size, null);
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
    	return super.findAll();
    }

    /** ✅ Get user by ID */
    public Optional<AppUserDto> getUserById(UUID id) {
    	return super.findById(id);
    }
}

