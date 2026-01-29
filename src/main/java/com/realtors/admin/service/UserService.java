package com.realtors.admin.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.realtors.admin.dto.AppUserDto;
import com.realtors.admin.dto.EmployeeCode;
import com.realtors.admin.dto.ListUserDto;
import com.realtors.admin.dto.PagedResult;
import com.realtors.admin.dto.UserBasicDto;
import com.realtors.admin.dto.UserDocumentDto;
import com.realtors.admin.dto.UserFlatDto;
import com.realtors.admin.dto.UserMiniDto;
import com.realtors.admin.dto.UserTreeDto;
import com.realtors.admin.dto.form.DynamicFormResponseDto;
import com.realtors.admin.dto.form.EditResponseDto;
import com.realtors.admin.dto.form.LookupDefinition;
import com.realtors.common.EnumConstants;
import com.realtors.common.service.AuditContext;
import com.realtors.common.service.AuditTrailService;
import com.realtors.common.service.FileStorageContext;
import com.realtors.common.service.FileSavingService;
import com.realtors.common.util.AppUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.postgresql.util.PGobject;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class UserService extends AbstractBaseService<AppUserDto, UUID> {

	private static final Logger logger = LoggerFactory.getLogger(AppUserService.class);
	ObjectMapper mapper = new ObjectMapper();

	@Autowired
	private JdbcTemplate jdbcTemplate;
	private final AuditTrailService audit;
	private final UserAuthService userAuthService;
	private final EmployeeCodeService codeService;
	private NamedParameterJdbcTemplate namedJdbcTemplate;
	private final FileSavingService fileService;

	private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	List<LookupDefinition> lookupDefs = List.of(
			new LookupDefinition("roles", "roles", "role_id", "role_name", "roleName"),
			new LookupDefinition("app_users", "app_users", "manager_id", "full_name", "managerName"));

	public UserService(JdbcTemplate jdbcTemplate, AuditTrailService audit, UserAuthService userAuthService,
			EmployeeCodeService codeService, FileSavingService fileService, NamedParameterJdbcTemplate namedJdbcTemplate) {
		super(AppUserDto.class, "app_users", jdbcTemplate, Set.of("role_name", "managerName"));
		// Add multiple foreign key lookups
		addDependentLookup("role_id", "roles", "role_id", "role_name", "roleName");
		addDependentLookup("manager_id", "app_users", "user_id", "full_name", "managerName");
		this.jdbcTemplate = jdbcTemplate;
		this.audit = audit;
		this.userAuthService = userAuthService;
		this.codeService = codeService;
		this.fileService = fileService;
		this.namedJdbcTemplate=namedJdbcTemplate; 
	}

	@Override
	protected String getIdColumn() {
		return "user_id";
	}

	private void getFilteredForm(DynamicFormResponseDto form) {
		AppUtil.filterUserLookUp(form, findSubordinates());
	}

	/** ✅ User form response */
	public DynamicFormResponseDto getUserFormData(boolean isCommonRole) {
		audit.auditAsync("users", null, EnumConstants.FORM.toString(), AppUtil.getCurrentUserId(),
				AuditContext.getIpAddress(), AuditContext.getUserAgent());
		DynamicFormResponseDto form = super.buildDynamicFormResponse();
		if (!isCommonRole) {
			getFilteredForm(form);
		}
		return form;
	}

	/** ✅ Update user form response */
	public EditResponseDto<AppUserDto> editUserResponse(UUID currentUserId, boolean isCommonRole) {
		Optional<AppUserDto> opt = super.findById(currentUserId);
		DynamicFormResponseDto form = super.buildDynamicFormResponse();
		if (!isCommonRole) {
			getFilteredForm(form);
		}
		audit.auditAsync("users", opt.isPresent() ? opt.get().getUserId() : null, EnumConstants.EDIT_FORM.toString(),
				AppUtil.getCurrentUserId(), AuditContext.getIpAddress(), AuditContext.getUserAgent());
		return opt.map(user -> new EditResponseDto<>(user, form)).orElse(null);
	}

	@Transactional("txManager")
	public void uploadDocument(UUID userId, String documentType, String documentNumber, MultipartFile file)
			throws Exception {

		if (file == null) {
			throw new IllegalArgumentException("No files uploaded");
		}
		FileStorageContext ctx = new FileStorageContext(file, userId, "users", "/documents/");
		String imagePathUrl = fileService.saveFile(ctx);

		UserDocumentDto docDto = new UserDocumentDto();
		docDto.setUserId(userId);
		docDto.setDocumentNumber(documentNumber);
		docDto.setDocumentType(documentType);
		docDto.setFileName(file.getOriginalFilename());
		docDto.setFilePath(imagePathUrl);
		docDto.setUploadedAt(LocalDateTime.now());
		docDto.setUploadedBy(AppUtil.getCurrentUserId());
		
		save(docDto);
	}

	private void save(UserDocumentDto d) {
		String sql = """
				    INSERT INTO user_documents
				    (user_id, document_type, document_number, file_name, file_path, uploaded_at, uploaded_by)
				    VALUES (?, ?, ?, ?, ?, ?, ?)
				""";
		jdbcTemplate.update(sql, d.getUserId(), d.getDocumentType(), d.getDocumentNumber(), d.getFileName(),
				d.getFilePath(), d.getUploadedAt(), d.getUploadedBy());
	}

	public UserDocumentDto findByDocumentId(Long docId) {
		List<UserDocumentDto> list = jdbcTemplate.query(
		        "SELECT * FROM user_documents WHERE document_id = ?",
		        new BeanPropertyRowMapper<>(UserDocumentDto.class),
		        docId
		    );
		    return list.isEmpty() ? null : list.get(0);
	}
	
	public void deleteDocument(Long docId) {
		UserDocumentDto dto = findByDocumentId(docId);
		UUID userId = dto.getUserId();
		fileService.deleteDocument(userId, "users", "/documents/", dto.getFileName());
		// delete DB record
		delete(docId);
	}
	
	private void delete(Long docId) {
		jdbcTemplate.update("DELETE FROM user_documents WHERE document_id = ?", docId);
	}
	
	public List<UserDocumentDto> findDocumentsByUserId(UUID userId) {
		String sql = "SELECT *  FROM user_documents WHERE user_id = ?";
		return jdbcTemplate.query(sql, new Object[] { userId }, (rs, rowNum) -> {
			UserDocumentDto d = new UserDocumentDto();
			d.setDocumentId(rs.getLong("document_id"));
			d.setUserId(UUID.fromString(rs.getString("user_id")));
			d.setDocumentType(rs.getString("document_type"));
			d.setDocumentNumber(rs.getString("document_number"));
			d.setFileName(rs.getString("file_name"));
			d.setFilePath(rs.getString("file_path"));
			d.setUploadedBy(rs.getString("uploaded_by") != null ? UUID.fromString(rs.getString("uploaded_by")) : null);
			d.setUploadedAt(rs.getTimestamp("uploaded_at").toLocalDateTime());
			return d;
		});
	}
	
	public List<UserMiniDto> getUsersByRole(UUID roleId) {
		String sql = "SELECT user_id, full_name, employee_id from app_users where role_id=?";
		return jdbcTemplate.query(sql, new Object[] { roleId }, (rs, rowNum) -> {
			UserMiniDto dto = new UserMiniDto();
			dto.setUserId(UUID.fromString(rs.getString("user_id")));
			dto.setFullName(rs.getString("full_name"));
			dto.setEmployeeId(rs.getString("employee_id"));
			return dto;
		});
	}

	public List<UserMiniDto> getUsersByRoles(Set<String> roles) {
		// 1. Check for empty roles to avoid SQL syntax errors (IN clause cannot be
		// empty)
		if (roles == null || roles.isEmpty()) {
			return Collections.emptyList();
		}
		String sql = """
				SELECT u.user_id, u.full_name, u.employee_id
				FROM app_users u
				JOIN roles r ON u.role_id = r.role_id
				WHERE r.finance_role IN (:roles)
				""";
		// 2. Wrap parameters in a Map
		Map<String, Object> params = Collections.singletonMap("roles", roles);
		return namedJdbcTemplate.query(sql, params, (rs, rowNum) -> {
			UserMiniDto dto = new UserMiniDto();
			dto.setUserId(UUID.fromString(rs.getString("user_id")));
			dto.setFullName(rs.getString("full_name"));
			dto.setEmployeeId(rs.getString("employee_id"));
			return dto;
		});
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
			throw new IllegalArgumentException("Register with different Email.");
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

		EmployeeCode code = codeService.generateEmployeeCode(data.getRoleId(), data.getManagerId(),
				data.getBranchCode());
		data.setEmployeeId(code.employeeId());
		data.setHierarchyCode(code.hierarchyCode());
		data.setSeqNo(code.seqNo());

		// meta is ALREADY a Map<String, Object> → no need to parse!
		Map<String, Object> meta = data.getMeta(); // ← Just get it directly
		// If you want a mutable copy (safe)
		Map<String, Object> metaMap = (meta != null) ? new HashMap<>(meta) // copy it
				: new HashMap<>(); // or empty map
		data.setMeta(metaMap);
		Map<String, Object> updatedMap = mapper.convertValue(data, Map.class);
		AppUserDto obj = super.createWithFiles(updatedMap);
		userAuthService.createUserAuth(obj.getUserId(), obj.getEmail(), null, obj.getRoleId(), obj.getMobile(), null);
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

	/** ✅ Update meta JSONB */
	public boolean updateMeta(UUID id, Map<String, Object> meta) {
		try {
			PGobject metaObj = new PGobject();
			metaObj.setType("jsonb");
			metaObj.setValue(mapper.writeValueAsString(meta));
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

	// Search Use data
	public List<ListUserDto> searchUsers(String searchText, boolean isCommonRole) {
		List<AppUserDto> data = super.search(searchText, List.of("full_name", "email", "mobile", "employee_id"), null);
		if (isCommonRole) {
			return toListUserDtos(data);
		} else {
			return findSelfAndSubordinates(AppUtil.getCurrentUserId(), searchText);
		}
	}

	private List<ListUserDto> findSelfAndSubordinates(UUID userId, String searchText) {
		String sql = """
					    WITH RECURSIVE subordinates AS (SELECT user_id
				        FROM app_users
				        WHERE user_id = :userId
				        UNION ALL
				        SELECT u.user_id
				        FROM app_users u
				        JOIN subordinates s ON u.manager_id = s.user_id
				    )
				    SELECT u.user_id, u.full_name, u.mobile, u.email, u.employee_id, u.manager_id, m.full_name AS manager_name, u.role_id,  r.role_name, u.status
				    FROM app_users u
				    LEFT JOIN app_users m ON m.user_id = u.manager_id
				    LEFT JOIN roles r ON r.role_id = u.role_id
				    JOIN subordinates s ON s.user_id = u.user_id
				    WHERE (
					    LOWER(u.full_name) LIKE :search OR
					    LOWER(u.email) LIKE :search OR
					    u.mobile LIKE :search OR
					    u.employee_id LIKE :search
					)
					ORDER BY u.full_name
				""";
		MapSqlParameterSource params = new MapSqlParameterSource().addValue("userId", userId).addValue("search",
				"%" + searchText.toLowerCase() + "%");
		List<ListUserDto> users = namedJdbcTemplate.query(sql, params,
				(rs, rowNum) -> new ListUserDto(rs.getObject("user_id", UUID.class), rs.getString("full_name"),
						rs.getString("mobile"), rs.getString("email"), rs.getString("employee_id"),
						rs.getObject("manager_id", UUID.class), rs.getString("manager_name"),
						rs.getObject("role_id", UUID.class), rs.getString("role_name"), rs.getString("status")));
		return users;
	}

	// Get Paged modules data thi
	public PagedResult<ListUserDto> getPaginatedUsers(int page, int size, boolean isCommonRole) {
		if (isCommonRole) {
			PagedResult<AppUserDto> paged = super.findAllPaginated(page, size, null);
			return new PagedResult<>(findAllUsers(paged.data(), true), paged.page(), paged.size(), paged.total(),
					paged.totalPages());
		}
		return findSelfAndSubordinatesPaginated(AppUtil.getCurrentUserId(), page, size);
	}

	private PagedResult<ListUserDto> findSelfAndSubordinatesPaginated(UUID userId, int page, int size) {
		int offset = (page - 1) * size;
		String dataSql = """
					    WITH RECURSIVE subordinates AS (
				        SELECT user_id
				        FROM app_users
				        WHERE user_id = :userId
				        UNION ALL
				        SELECT u.user_id
				        FROM app_users u
				        JOIN subordinates s ON u.manager_id = s.user_id
				    )
				    SELECT u.user_id, u.full_name, u.mobile, u.email, u.employee_id, u.manager_id, m.full_name AS manager_name, u.role_id,  r.role_name, u.status
				    FROM app_users u
				    LEFT JOIN app_users m ON m.user_id = u.manager_id
				    LEFT JOIN roles r ON r.role_id = u.role_id
				    JOIN subordinates s ON s.user_id = u.user_id
				    ORDER BY u.full_name
				    LIMIT :limit OFFSET :offset
				""";

		String countSql = """
				    WITH RECURSIVE subordinates AS (
				        SELECT user_id
				        FROM app_users
				        WHERE user_id = :userId
				        UNION ALL
				        SELECT u.user_id
				        FROM app_users u
				        JOIN subordinates s ON u.manager_id = s.user_id
				    )
				    SELECT COUNT(*) FROM subordinates
				""";

		MapSqlParameterSource params = new MapSqlParameterSource().addValue("userId", userId).addValue("limit", size)
				.addValue("offset", offset);

		List<ListUserDto> users = namedJdbcTemplate.query(dataSql, params,
				(rs, rowNum) -> new ListUserDto(rs.getObject("user_id", UUID.class), rs.getString("full_name"),
						rs.getString("mobile"), rs.getString("email"), rs.getString("employee_id"),
						rs.getObject("manager_id", UUID.class), rs.getString("manager_name"),
						rs.getObject("role_id", UUID.class), rs.getString("role_name"), rs.getString("status")));
		int total = namedJdbcTemplate.queryForObject(countSql, params, Integer.class);
		int totalPages = (int) Math.ceil((double) total / size);

		return new PagedResult<>(users, page, size, total, totalPages);
	}

	private List<ListUserDto> toListUserDtos(List<AppUserDto> users) {
		return users.stream()
				.map(u -> new ListUserDto(u.getUserId(), u.getFullName(), u.getMobile(), u.getEmail(),
						u.getEmployeeId(), u.getManagerId(), u.getManagerName(), u.getRoleId(), u.getRoleName(),
						u.getStatus()))
				.toList();
	}

	/** ✅ Get all active users */
	public List<ListUserDto> getAllUsers(boolean isCommonRole) {
		return findAllUsers(super.findAll(), isCommonRole);
	}

	private List<ListUserDto> findAllUsers(List<AppUserDto> users, boolean isCommonRole) {
		if (isCommonRole) {
			return toListUserDtos(users);
		}
		Set<UUID> allowedSet = new HashSet<>(findSelfAndSubordinateIds(AppUtil.getCurrentUserId()));
		return users.stream().filter(u -> allowedSet.contains(u.getUserId()))
				.map(u -> new ListUserDto(u.getUserId(), u.getFullName(), u.getMobile(), u.getEmail(),
						u.getEmployeeId(), u.getManagerId(), u.getManagerName(), u.getRoleId(), u.getRoleName(),
						u.getStatus()))
				.toList();
	}

	private List<UUID> findSelfAndSubordinateIds(UUID userId) {
		String sql = """
				    WITH RECURSIVE subordinates AS (
				        SELECT user_id
				        FROM app_users
				        WHERE user_id = :userId
				        UNION ALL
				        SELECT u.user_id
				        FROM app_users u
				        JOIN subordinates s ON u.manager_id = s.user_id
				    )
				    SELECT user_id FROM subordinates
				""";
		return namedJdbcTemplate.queryForList(sql, new MapSqlParameterSource("userId", userId), UUID.class);
	}

	/** ✅ Get user by ID */
	public Optional<AppUserDto> getUserById(UUID id) {
		Optional<AppUserDto> opt = super.findById(id);
		audit.auditAsync("users", opt.isPresent() ? opt.get().getUserId() : null, EnumConstants.BY_ID.toString(),
				AppUtil.getCurrentUserId(), AuditContext.getIpAddress(), AuditContext.getUserAgent());

		return super.findById(id);
	}

	public boolean userExists(String email, String mobile) {
		StringBuilder sql = new StringBuilder("""
				    SELECT COUNT(*)
				    FROM app_users
				    WHERE status = 'ACTIVE'
				""");

		List<Object> params = new ArrayList<>();
		if (email != null && !email.isBlank()) {
			sql.append(" AND email = ?");
			params.add(email);
		}
		if (mobile != null && !mobile.isBlank()) {
			sql.append(" AND mobile = ?");
			params.add(mobile);
		}
		// If neither provided, nothing to check
		if (params.isEmpty()) {
			return false;
		}
		Integer count = jdbcTemplate.queryForObject(sql.toString(), params.toArray(), Integer.class);
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

	public List<UserTreeDto> findUserTree() {
		UUID rootUserId = AppUtil.getCurrentUserId();
		String sql = """
				    SELECT user_id, full_name, manager_id, email, mobile, employee_id
				    FROM app_users
				    WHERE status = 'ACTIVE'
				""";
		List<UserFlatDto> flatList = namedJdbcTemplate.query(sql,
				(rs, rowNum) -> new UserFlatDto(rs.getObject("user_id", UUID.class), rs.getString("full_name"),
						rs.getObject("manager_id", UUID.class), rs.getString("email"), rs.getString("mobile"),
						rs.getString("employee_id")));

		UserTreeDto root = buildTree(flatList, rootUserId);
		return root == null ? List.of() : List.of(root);
	}

	private UserTreeDto buildTree(List<UserFlatDto> flatList, UUID rootUserId) {
		Set<UUID> allowedIds = collectDescendants(rootUserId, flatList);
		Map<UUID, UserTreeDto> map = new HashMap<>();

		// Create nodes ONLY for allowed users
		for (UserFlatDto u : flatList) {
			if (allowedIds.contains(u.getUserId())) {
				map.put(u.getUserId(), new UserTreeDto(u.getUserId(), u.getFullName(), u.getEmployeeId()));
			}
		}
		// Build hierarchy ONLY within subtree
		for (UserFlatDto u : flatList) {
			if (!allowedIds.contains(u.getUserId()))
				continue;

			UserTreeDto node = map.get(u.getUserId());
			UUID managerId = u.getManagerId();
			if (managerId != null && map.containsKey(managerId)) {
				map.get(managerId).getChildren().add(node);
			}
		}
		return map.get(rootUserId);
	}

	private Set<UUID> collectDescendants(UUID rootUserId, List<UserFlatDto> flatList) {
		Map<UUID, List<UUID>> childrenMap = new HashMap<>();
		for (UserFlatDto u : flatList) {
			if (u.getManagerId() != null) {
				childrenMap.computeIfAbsent(u.getManagerId(), k -> new ArrayList<>()).add(u.getUserId());
			}
		}
		Set<UUID> result = new HashSet<>();
		Deque<UUID> stack = new ArrayDeque<>();
		stack.push(rootUserId);

		while (!stack.isEmpty()) {
			UUID current = stack.pop();
			result.add(current);

			for (UUID child : childrenMap.getOrDefault(current, List.of())) {
				if (!result.contains(child)) {
					stack.push(child);
				}
			}
		}
		return result;
	}

	public List<UserBasicDto> findSubordinates() {
		UUID userId = AppUtil.getCurrentUserId();
		String sql = """
					WITH RECURSIVE subordinates AS (
					    SELECT user_id, full_name, employee_id
					    FROM app_users
					    WHERE user_id = :userId
					    UNION ALL
					    SELECT u.user_id,u.full_name,u.employee_id
					    FROM app_users u
					    JOIN subordinates s  ON u.manager_id = s.user_id
					)
					SELECT user_id, full_name, employee_id FROM subordinates;
				""";
		MapSqlParameterSource params = new MapSqlParameterSource("userId", userId);
		return namedJdbcTemplate.query(sql, params,
				(rs, rowNum) -> new UserBasicDto(rs.getObject("user_id", UUID.class), rs.getString("full_name"),
						rs.getString("employee_id")));
	}
}
