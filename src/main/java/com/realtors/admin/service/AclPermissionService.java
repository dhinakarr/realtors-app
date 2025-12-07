package com.realtors.admin.service;

import com.realtors.admin.dto.AclPermissionDto;
import com.realtors.admin.dto.FeatureDto;
import com.realtors.admin.dto.FeaturePermissionDto;
import com.realtors.admin.dto.ModuleDto;
import com.realtors.admin.dto.ModulePermissionDto;
import com.realtors.admin.dto.PagedResult;
import com.realtors.admin.dto.RoleDto;
import com.realtors.admin.dto.form.FeatureFormDto;
import com.realtors.admin.dto.form.LookupDefinition;
import com.realtors.admin.dto.form.ModuleFormDto;
import com.realtors.admin.dto.form.PermissionFormDto;
import com.realtors.admin.dto.form.RoleFormDto;
import com.realtors.common.EnumConstants;
import com.realtors.common.service.AuditContext;
import com.realtors.common.service.AuditTrailService;
import com.realtors.common.util.AclPermissionRowMapper;
import com.realtors.common.util.AppUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AclPermissionService extends AbstractBaseService<AclPermissionDto, UUID> {

	private static final Logger log = LoggerFactory.getLogger(AclPermissionService.class);
//	private static final String CACHE_NAME = "aclPermissions";
	private static final String TABLE_NAME = "acl_permissions";
	private static final String ID_COLUMNS = "role_id,permission_id";

	private final JdbcTemplate jdbcTemplate;
	private RoleService roleService;
	private ModuleService moduleService;
	private FeatureService featureService;
	private final AuditTrailService audit;

	List<LookupDefinition> lookupDefs = List.of(
			new LookupDefinition("roles", "roles", "role_id", "role_name", "roleName"),
			new LookupDefinition("features", "features", "feature_id", "feature_name", "featureName"));

	public AclPermissionService(JdbcTemplate jdbcTemplate, RoleService roleService, ModuleService moduleService,
			FeatureService featureService, AuditTrailService audit) {
		super(AclPermissionDto.class, TABLE_NAME, jdbcTemplate);
		this.jdbcTemplate = jdbcTemplate;
		this.roleService = roleService;
		this.moduleService = moduleService;
		this.featureService = featureService;
		this.audit = audit;
		addDependentLookup("role_id", "roles", "role_id", "role_name", "roleName");
		addDependentLookup("feature_id", "features", "feature_id", "feature_name", "featureName");
	}

	@Override
	protected String getIdColumn() {
		return ID_COLUMNS;
	}

	@Override
//	@Cacheable(value = CACHE_NAME, key = "'ALL'")
	public List<AclPermissionDto> findAll() {
		List<AclPermissionDto> retVal = super.findAll();
		audit.auditAsync(TABLE_NAME, retVal.getFirst().getPermissionId(), EnumConstants.GET_ALL.toString(), 
				AppUtil.getCurrentUserId(), AuditContext.getIpAddress(), AuditContext.getUserAgent());
		return retVal;
	}

	public List<ModulePermissionDto> getAllByModules() {
		List<ModulePermissionDto> listModules = findPermissionsByRole(null);
		audit.auditAsync(TABLE_NAME, listModules.getFirst().moduleId(), EnumConstants.GET_ALL.toString(), 
				AppUtil.getCurrentUserId(), AuditContext.getIpAddress(), AuditContext.getUserAgent());
		return listModules;
	}

	public PagedResult<AclPermissionDto> getAllPaginated(int page, int size) {
		/*
		 * return new FeatureListResponseDto<>( "Permissions", "table",
		 * List.of("Role Name", "Feature Name", "Create", "Read", "Update", "Delete",
		 * "Status"), Map.ofEntries( Map.entry("Feature Name", "featureName"),
		 * Map.entry("Role Name", "roleName"), Map.entry("Create", "canCreate"),
		 * Map.entry("Read", "canRead"), Map.entry("Update", "canUpdate"),
		 * Map.entry("Delete", "canDelete"), Map.entry("Status", "status")),
		 * "permissionId", false, // pagination enabled super.findAllPaginated(page,
		 * size, null), // <-- MUST return PagedResult<AppUserDto>
		 * super.getLookupData(lookupDefs) // <-- fully dynamic lookup map );
		 */
		PagedResult<AclPermissionDto> retObj = super.findAllPaginated(page, size, null);
		UUID record = retObj.data().getFirst().getPermissionId();
		audit.auditAsync(TABLE_NAME, record, EnumConstants.PAGED.toString(), 
				AppUtil.getCurrentUserId(), AuditContext.getIpAddress(), AuditContext.getUserAgent());
		return retObj;
	}

	@Override
//    @Cacheable(value =CACHE_NAME, key = "#id")
	public Optional<AclPermissionDto> findById(UUID id) {
		Optional<AclPermissionDto> dto = super.findById(id);
		audit.auditAsync(TABLE_NAME, dto.isPresent() ? dto.get().getPermissionId() : null, EnumConstants.BY_ID.toString(), 
				AppUtil.getCurrentUserId(), AuditContext.getIpAddress(), AuditContext.getUserAgent());
		return dto;
	}

//    @Cacheable(value = CACHE_NAME, key = "'role:' + #roleId")
	public List<AclPermissionDto> findAllByRole(UUID roleId) {
		String sql = "SELECT * FROM acl_permissions WHERE role_id = ?";
		List<AclPermissionDto> list = jdbcTemplate.query(sql, new AclPermissionRowMapper(), roleId);
		audit.auditAsync(TABLE_NAME, roleId, EnumConstants.BY_ID.toString(), 
				AppUtil.getCurrentUserId(), AuditContext.getIpAddress(), AuditContext.getUserAgent());
		return list;
	}

	@Override
//    @CachePut(value = CACHE_NAME, key = "#result.permissionId")
//    @CacheEvict(value = CACHE_NAME, key = "'role:' + #dto['roleId']")
	public AclPermissionDto create(AclPermissionDto dto) {
		AclPermissionDto data = super.create(dto);
		audit.auditAsync(TABLE_NAME, data.getPermissionId(), EnumConstants.CREATE.toString(), 
				AppUtil.getCurrentUserId(), AuditContext.getIpAddress(), AuditContext.getUserAgent());
		
		return data;
	}

	@Override
//    @CachePut(value = CACHE_NAME, key = "#id")
//    @CacheEvict(value = CACHE_NAME, key = "'role:' + #dto['roleId']")
	public AclPermissionDto update(UUID id, AclPermissionDto dto) {
		AclPermissionDto data = super.update(id, dto);
		audit.auditAsync(TABLE_NAME, data.getPermissionId(), EnumConstants.UPDATE.toString(), 
				AppUtil.getCurrentUserId(), AuditContext.getIpAddress(), AuditContext.getUserAgent());
		return data;
	}

	/*
	 * @CachePut( value = CACHE_NAME, key =
	 * "T(String).valueOf(#dto['roleId']) + ':' + T(String).valueOf(#id)" )
	 * 
	 * @CacheEvict(value = CACHE_NAME, key = "'role:' + #dto['roleId'] + ':list'")
	 */
	public AclPermissionDto partialUpdate(UUID id, Map<String, Object> dto) {
		UUID roleId = asUuid(dto.get("roleId"));
		Map<String, Object> compositeId = Map.of("role_id", roleId, "permission_id", id);
		AclPermissionDto data = super.patch(compositeId, dto);
		audit.auditAsync(TABLE_NAME, id, EnumConstants.PATCH.toString(), 
				AppUtil.getCurrentUserId(), AuditContext.getIpAddress(), AuditContext.getUserAgent());
		return data;
	}

	private UUID asUuid(Object value) {
		if (value instanceof UUID u)
			return u;
		if (value instanceof String s)
			return UUID.fromString(s);
		throw new IllegalArgumentException("Invalid UUID: " + value);
	}

//    @CacheEvict(value = CACHE_NAME, allEntries = true)
	public boolean delete(UUID id) {
		boolean flag = super.softDelete(id);
		audit.auditAsync(TABLE_NAME, id, EnumConstants.DELETE.toString(), 
				AppUtil.getCurrentUserId(), AuditContext.getIpAddress(), AuditContext.getUserAgent());
		return flag;
	}

//    @CacheEvict(value = CACHE_NAME, key = "'ALL'")
	public void clearAllCache() {
		log.info("Manually evicting all permission cache entries");
	}

	public List<AclPermissionDto> searchPage(String searchText, String status) {
		List<String> searchFields = List.of();
		List<AclPermissionDto> list = super.search(searchText, searchFields, status);
		audit.auditAsync(TABLE_NAME, null, EnumConstants.SEARCH.toString(), 
				AppUtil.getCurrentUserId(), AuditContext.getIpAddress(), AuditContext.getUserAgent());
		return list;
	}

	/*
	 * // @Cacheable(value = CACHE_NAME, key = "'role:' + #roleId") public
	 * List<ModulePermissionDto> findPermissionsByRole(UUID roleId) {
	 * List<Map<String, Object>> result =
	 * jdbcTemplate.queryForList(permissionbyRoleQuery, roleId); Map<UUID,
	 * ModulePermissionDto> moduleMap = new LinkedHashMap<>(); for (Map<String,
	 * Object> row : result) { UUID moduleId = (UUID) row.get("module_id"); String
	 * moduleName = (String) row.get("module_name");
	 * 
	 * FeaturePermissionDto feature = new FeaturePermissionDto((UUID)
	 * row.get("permission_id"), (UUID) row.get("role_id"), (String)
	 * row.get("role_name"), (UUID) row.get("feature_id"), (String)
	 * row.get("feature_name"), (String) row.get("url"), (String)
	 * row.get("feature_type"), (Boolean) row.get("can_create"), (Boolean)
	 * row.get("can_read"), (Boolean) row.get("can_update"), (Boolean)
	 * row.get("can_delete"), (String) row.get("status"));
	 * 
	 * moduleMap.computeIfAbsent(moduleId, id -> new ModulePermissionDto(id,
	 * moduleName, new ArrayList<>())) .features().add(feature); }
	 * audit.auditAsync(TABLE_NAME, roleId, "GET By Role",
	 * AppUtil.getCurrentUserId(), AuditContext.getIpAddress(),
	 * AuditContext.getUserAgent()); return new ArrayList<>(moduleMap.values()); }
	 */
	// ------------------------------------------------------------
	// BULK INSERT (fast batch)
	// ------------------------------------------------------------
//    @CacheEvict(value = CACHE_NAME, key = "'role:' + #roleId")
	public boolean bulkInsert(UUID roleId, List<AclPermissionDto> permissions) {
		int[] rows = new int[0];
		List<AclPermissionDto> permissionsToProcess = permissions.stream()
		        .filter(p -> p.isCanCreate() || p.isCanRead() || p.isCanUpdate() || p.isCanDelete())
		        .collect(Collectors.toList());

		    if (permissionsToProcess.isEmpty())  {
		    	return true; 
		    }
		
		String sql = """
		        INSERT INTO acl_permissions
		        (role_id, feature_id, can_create, can_read, can_update, can_delete, created_by, updated_by, status, created_at, updated_at)
		        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
		    """;
		UUID userId = AppUtil.getCurrentUserId();
		rows = jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
			@Override
			public void setValues(PreparedStatement ps, int i) throws SQLException {
				AclPermissionDto p = permissionsToProcess.get(i);

				ps.setObject(1, roleId);                 // role
	            ps.setObject(2, p.getFeatureId());       // feature
	            ps.setBoolean(3, p.isCanCreate());
	            ps.setBoolean(4, p.isCanRead());
	            ps.setBoolean(5, p.isCanUpdate());
	            ps.setBoolean(6, p.isCanDelete());
	            ps.setObject(7, userId);                 // created_by
	            ps.setObject(8, userId); 
	            ps.setString(9, "ACTIVE");
	            ps.setObject(10, OffsetDateTime.now());
	            ps.setObject(11, OffsetDateTime.now());
			}
			@Override
			public int getBatchSize() {
				return permissionsToProcess.size();
			}
		});
		
		audit.auditAsync(TABLE_NAME, roleId, EnumConstants.CREATE_BULK.toString(), 
				AppUtil.getCurrentUserId(), AuditContext.getIpAddress(), AuditContext.getUserAgent());
		return rows.length > 0 ? true : false;
	}

	// ------------------------------------------------------------
	// BULK UPDATE (fast batch)
	// ------------------------------------------------------------
//    @CacheEvict(value = CACHE_NAME, key = "'role:' + #roleId")
	public void bulkUpdate(UUID roleId, List<AclPermissionDto> permissions) {

		String sql = """
				    UPDATE acl_permissions SET
				        feature_id = ?,
				        can_create = ?,
				        can_read = ?,
				        can_update = ?,
				        can_delete = ?,
				        status = ?,
				        updated_at = CURRENT_TIMESTAMP,
				        updated_by = ?
				    WHERE role_id = ? AND permission_id = ?
				""";
		UUID userId = AppUtil.getCurrentUserId();
		jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
			@Override
			public void setValues(PreparedStatement ps, int i) throws SQLException {
				AclPermissionDto p = permissions.get(i);

				ps.setObject(1, p.getFeatureId());
				ps.setBoolean(2, p.isCanCreate());
				ps.setBoolean(3, p.isCanRead());
				ps.setBoolean(4, p.isCanUpdate());
				ps.setBoolean(5, p.isCanDelete());
				ps.setString(6, p.getStatus());
				ps.setObject(7, userId);
				ps.setObject(8, roleId);
				ps.setObject(9, p.getPermissionId());
			}

			public int getBatchSize() {
				return permissions.size();
			}
		});
		audit.auditAsync(TABLE_NAME, roleId, EnumConstants.UPDATE_BULK.toString(), 
				AppUtil.getCurrentUserId(), AuditContext.getIpAddress(), AuditContext.getUserAgent());
	}

//    @Cacheable(value = CACHE_NAME, key = "'role:' + #roleId")
	public List<ModulePermissionDto> findPermissionsByRole(UUID roleId) {
		String sql = (roleId == null) ? PERMISSIONS_FOR_ALL : permissionbyRoleQuery;
		List<Map<String, Object>> rows = (roleId == null) ? jdbcTemplate.queryForList(sql)
				: jdbcTemplate.queryForList(sql, roleId);
		List<ModulePermissionDto> list = mapToModulePermissionDto(rows);
		audit.auditAsync(TABLE_NAME, roleId, "GET By Role", 
				AppUtil.getCurrentUserId(), AuditContext.getIpAddress(), AuditContext.getUserAgent());
		return list;
	}

	private List<ModulePermissionDto> mapToModulePermissionDto(List<Map<String, Object>> rows) {
		Map<UUID, ModulePermissionDto> moduleMap = new LinkedHashMap<>();

		for (Map<String, Object> row : rows) {

			UUID moduleId = (UUID) row.get("module_id");
			String moduleName = (String) row.get("module_name");

			FeaturePermissionDto feature = new FeaturePermissionDto((UUID) row.get("permission_id"),
					(UUID) row.get("role_id"), (String) row.get("role_name"), (UUID) row.get("feature_id"),
					(String) row.get("feature_name"), (String) row.get("url"), (String) row.get("feature_type"),
					Boolean.TRUE.equals(row.get("can_create")), Boolean.TRUE.equals(row.get("can_read")),
					Boolean.TRUE.equals(row.get("can_update")), Boolean.TRUE.equals(row.get("can_delete")),
					(String) row.get("status"));

			moduleMap.computeIfAbsent(moduleId, id -> new ModulePermissionDto(id, moduleName, new ArrayList<>()))
					.features().add(feature);
		}

		return new ArrayList<>(moduleMap.values());
	}

	private static final String permissionbyRoleQuery = """
			SELECT m.module_id, m.module_name, p.permission_id, r.role_id, r.role_name, f.feature_id, f.feature_name, f.url, f.feature_type,
			    p.can_create, p.can_read, p.can_update, p.can_delete, p.status
			FROM acl_permissions p
			JOIN roles r ON r.role_id = p.role_id JOIN features f ON f.feature_id = p.feature_id
			JOIN modules m ON m.module_id = f.module_id WHERE p.status = 'ACTIVE' AND r.status = 'ACTIVE'
			  AND f.status = 'ACTIVE' AND m.status = 'ACTIVE' AND r.role_id = ?
			ORDER BY m.module_name, f.feature_name
			""";

	// Query for ALL permissions (no role filtering)
	private static final String PERMISSIONS_FOR_ALL = """
			    SELECT
			        m.module_id, m.module_name, p.permission_id, r.role_id, r.role_name, f.feature_id, f.feature_name, f.url, f.feature_type,
			        p.can_create, p.can_read, p.can_update, p.can_delete, p.status
			    FROM acl_permissions p
			    JOIN roles r ON r.role_id = p.role_id JOIN features f ON f.feature_id = p.feature_id
			    JOIN modules m ON m.module_id = f.module_id
			    WHERE p.status = 'ACTIVE' AND r.status = 'ACTIVE' AND f.status = 'ACTIVE'
			      AND m.status = 'ACTIVE' AND r.role_id = p.role_id ORDER BY m.module_name, f.feature_name
			""";

	public PermissionFormDto getPermissionFormData() {
		List<RoleDto> roles = roleService.getAllRoles();
		List<ModuleDto> modules = moduleService.getAllModules();
		List<FeatureDto> features = featureService.getAllFeatures();
		List<RoleFormDto> roleDto = roles.stream().map(r -> new RoleFormDto(r.getRoleId(), r.getRoleName()))
				.collect(Collectors.toList());

		Map<UUID, List<FeatureFormDto>> featureMap = features.stream()
				.collect(Collectors.groupingBy(FeatureDto::getModuleId, Collectors
						.mapping(f -> new FeatureFormDto(f.getFeatureId(), f.getFeatureName()), Collectors.toList())));

		List<ModuleFormDto> moduleDtos = modules.stream().map(m -> new ModuleFormDto(m.getModuleId(), m.getModuleName(),
				featureMap.getOrDefault(m.getModuleId(), List.of()))).toList();
		return new PermissionFormDto(roleDto, moduleDtos);
	}
}
