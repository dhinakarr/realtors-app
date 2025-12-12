package com.realtors.admin.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.realtors.admin.dto.AppUserDto;
import com.realtors.admin.dto.PagedResult;
import com.realtors.admin.dto.form.DynamicFormResponseDto;
import com.realtors.admin.dto.form.EditResponseDto;
import com.realtors.admin.dto.form.LookupDefinition;
import com.realtors.common.EnumConstants;
import com.realtors.common.service.AuditContext;
import com.realtors.common.service.AuditTrailService;
import com.realtors.common.util.AppUtil;

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
public class UserService extends AbstractBaseService<AppUserDto, UUID> {

	private static final Logger logger = LoggerFactory.getLogger(AppUserService.class);
	ObjectMapper mapper = new ObjectMapper();

	@Autowired
	private JdbcTemplate jdbcTemplate;
	private final AuditTrailService audit;
	private final UserAuthService userAuthService;

	@Autowired
	private ObjectMapper objectMapper;
	private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	List<LookupDefinition> lookupDefs = List.of(
			new LookupDefinition("roles", "roles", "role_id", "role_name", "roleName"),
			new LookupDefinition("app_users", "app_users", "manager_id", "full_name", "managerName"));

	public UserService(JdbcTemplate jdbcTemplate, AuditTrailService audit, UserAuthService userAuthService) {
		super(AppUserDto.class, "app_users", jdbcTemplate, Set.of("role_name", "managerName"));
		// Add multiple foreign key lookups
		addDependentLookup("role_id", "roles", "role_id", "role_name", "roleName");
		addDependentLookup("manager_id", "app_users", "user_id", "full_name", "managerName");
		this.jdbcTemplate = jdbcTemplate;
		this.audit = audit;
		this.userAuthService = userAuthService;
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
				dto.setMeta(objectMapper.readValue(metaObj.getValue(), new TypeReference<Map<String, Object>>() {
				}));
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
		audit.auditAsync("users", null, EnumConstants.FORM.toString(), AppUtil.getCurrentUserId(),
				AuditContext.getIpAddress(), AuditContext.getUserAgent());
		return super.buildDynamicFormResponse();
	}

	/** ✅ Update user form response */
	public EditResponseDto<AppUserDto> editUserResponse(UUID currentUserId) {
		Optional<AppUserDto> opt = super.findById(currentUserId);
		DynamicFormResponseDto form = super.buildDynamicFormResponse();
		audit.auditAsync("users", opt.isPresent() ? opt.get().getUserId() : null, EnumConstants.EDIT_FORM.toString(),
				AppUtil.getCurrentUserId(), AuditContext.getIpAddress(), AuditContext.getUserAgent());
		return opt.map(user -> new EditResponseDto<>(user, form)).orElse(null);
	}

	/** ✅ Create User */
	public AppUserDto createUser(AppUserDto dto) {
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
		String hashedPassword = passwordEncoder
				.encode(dto.getPasswordHash() == null ? "Test@123" : dto.getPasswordHash());
		dto.setPasswordHash(hashedPassword);
		AppUserDto data = super.create(dto);

		userAuthService.createUserAuth(data.getUserId(), data.getEmail(), hashedPassword, data.getRoleId(), null);

		audit.auditAsync("users", data.getUserId(), EnumConstants.CREATE.toString(), AppUtil.getCurrentUserId(),
				AuditContext.getIpAddress(), AuditContext.getUserAgent());

		return data;
	}

	/** ✅ Update user */
	public AppUserDto updateUser(AppUserDto dto, UUID currentUserId) {
		AppUserDto data = super.update(currentUserId, dto);
		audit.auditAsync("users", data.getUserId(), EnumConstants.UPDATE.toString(), AppUtil.getCurrentUserId(),
				AuditContext.getIpAddress(), AuditContext.getUserAgent());
		return data;
	}

	// ---------------- CREATE ----------------
	public AppUserDto createWithFiles(AppUserDto data, MultipartFile profileImage) {
		// Hash password
		String hashedPassword = passwordEncoder.encode("Test@123");
		data.setPasswordHash(hashedPassword);

		if (userExists(data.getEmail(), data.getMobile())) {
			throw new IllegalArgumentException("Register with different mobile.");
		}

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
		Map<String, Object> meta = data.getMeta(); // ← Just get it directly
		// If you want a mutable copy (safe)
		Map<String, Object> metaMap = (meta != null) ? new HashMap<>(meta) // copy it
				: new HashMap<>(); // or empty map
		data.setMeta(metaMap);
		Map<String, Object> updatedMap = mapper.convertValue(data, Map.class);
		AppUserDto obj = super.createWithFiles(updatedMap);
		userAuthService.createUserAuth(obj.getUserId(), obj.getEmail(), null, obj.getRoleId(), null);
		audit.auditAsync("users", obj.getUserId(), EnumConstants.CREATE_WITH_FILES.toString(),
				AppUtil.getCurrentUserId(), AuditContext.getIpAddress(), AuditContext.getUserAgent());
		// You can use a GenericInsertUtil that supports files
		return obj;
	}

	// ---------------- UPDATE ----------------
	public AppUserDto updateWithFiles(UUID id, Map<String, Object> updates) {
		AppUserDto data = super.patchUpdateWithFile(id, updates);
		audit.auditAsync("users", data.getUserId(), EnumConstants.PATCH.toString(), AppUtil.getCurrentUserId(),
				AuditContext.getIpAddress(), AuditContext.getUserAgent());
		return data;
	}

	/** ✅ Soft delete */
	public boolean softDeleteUser(UUID userId) {
		audit.auditAsync("users", userId, EnumConstants.DELETE.toString(), AppUtil.getCurrentUserId(),
				AuditContext.getIpAddress(), AuditContext.getUserAgent());
		return super.softDelete(userId);
	}

	public AppUserDto partialUpdate(UUID id, Map<String, Object> dto) {
		AppUserDto data = super.patch(id, dto);
		audit.auditAsync("users", data.getUserId(), EnumConstants.PATCH.toString(), AppUtil.getCurrentUserId(),
				AuditContext.getIpAddress(), AuditContext.getUserAgent());
		return data;
	}

	// Search Use data
	public List<AppUserDto> searchUsers(String searchText) {

		audit.auditAsync("users", null, EnumConstants.SEARCH.toString(), AppUtil.getCurrentUserId(),
				AuditContext.getIpAddress(), AuditContext.getUserAgent());
		return super.search(searchText, List.of("full_name", "email", "mobile"), null);
	}

	// Get Paged modules data thi
	public PagedResult<AppUserDto> getPaginatedUsers(int page, int size) {
		PagedResult<AppUserDto> data = super.findAllPaginated(page, size, null);
		audit.auditAsync("users", data.data().getFirst().getUserId(), EnumConstants.PAGED.toString(),
				AppUtil.getCurrentUserId(), AuditContext.getIpAddress(), AuditContext.getUserAgent());
		return data;
	}

	/** ✅ Update meta JSONB */
	public boolean updateMeta(UUID id, Map<String, Object> meta) {
		try {
			PGobject metaObj = new PGobject();
			metaObj.setType("jsonb");
			metaObj.setValue(objectMapper.writeValueAsString(meta));
			int rows = jdbcTemplate.update(
					"UPDATE app_users SET meta = ?, updated_at = CURRENT_TIMESTAMP WHERE user_id = ?", metaObj, id);
			return rows > 0;
		} catch (Exception e) {
			logger.error("Error updating meta", e);
			return false;
		}
	}

	/** ✅ Update last login */
	public boolean updateLastLogin(UUID userId) {
		int rows = jdbcTemplate.update("UPDATE app_users SET last_login = CURRENT_TIMESTAMP WHERE user_id = ?", userId);
		audit.auditAsync("users", userId, EnumConstants.UPDATE.toString(), AppUtil.getCurrentUserId(),
				AuditContext.getIpAddress(), AuditContext.getUserAgent());
		return rows > 0;
	}

	/** ✅ Get all active users */
	public List<AppUserDto> getAllUsers() {
		List<AppUserDto> list = super.findAll();
		audit.auditAsync("users", list.getFirst().getUserId(), EnumConstants.GET_ALL.toString(),
				AppUtil.getCurrentUserId(), AuditContext.getIpAddress(), AuditContext.getUserAgent());
		return list;
	}

	/** ✅ Get user by ID */
	public Optional<AppUserDto> getUserById(UUID id) {
		Optional<AppUserDto> opt = super.findById(id);
		audit.auditAsync("users", opt.isPresent() ? opt.get().getUserId() : null, EnumConstants.BY_ID.toString(),
				AppUtil.getCurrentUserId(), AuditContext.getIpAddress(), AuditContext.getUserAgent());

		return super.findById(id);
	}

	public boolean userExists(String email, String mobile) {
		String sql = """
				SELECT COUNT(*)
				FROM app_users
				WHERE status = 'ACTIVE'
				  AND (email = ? OR mobile = ?)
				""";
		Integer count = jdbcTemplate.queryForObject(sql, Integer.class, email, mobile);
		return count != null && count > 0;
	}

	public List<UUID> getHierarchyUpwards(UUID userId) {

		List<UUID> list = new ArrayList<>();
		UUID current = userId;

		String sql = "SELECT manager_id FROM app_users WHERE user_id = ?";

		while (true) {
			List<UUID> results = jdbcTemplate.query(sql, new Object[] { current },
					(rs, rowNum) -> (UUID) rs.getObject("manager_id"));

			if (results.isEmpty())
				break; // no such user
			UUID managerId = results.get(0);

			if (managerId == null)
				break; // reached top of hierarchy

			list.add(managerId);
			current = managerId;
		}
		return list;
	}

	public void findSubordinatesRecursive(UUID managerId, List<UUID> result) {
		String sql = "SELECT user_id FROM app_users WHERE manager_id = ?";
		List<UUID> subs = jdbcTemplate.queryForList(sql, UUID.class, managerId);

		for (UUID sub : subs) {
			result.add(sub);
			findSubordinatesRecursive(sub, result);
		}
	}
}
