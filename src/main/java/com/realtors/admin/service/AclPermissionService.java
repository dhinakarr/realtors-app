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
import com.realtors.common.service.AuditTrailService;
import com.realtors.common.util.AclPermissionRowMapper;
import com.realtors.common.util.AppUtil;
import com.realtors.common.validator.PermissionAction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

@CacheConfig(cacheNames = "aclPermissions")
@Service
public class AclPermissionService extends AbstractBaseService<AclPermissionDto, UUID> {

	private static final Logger log = LoggerFactory.getLogger(AclPermissionService.class);
	private static final String TABLE_NAME = "acl_permissions";
	private static final String ID_COLUMNS = "role_id,permission_id";

	private static final String KEY_ALL = "'acl:all'";
	private static final String KEY_ROLE = "'acl:role:' + #roleId";

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
	@Cacheable(key = KEY_ALL)
	public List<AclPermissionDto> findAll() {
		List<AclPermissionDto> retVal = super.findAll();
		return retVal;
	}

	public List<ModulePermissionDto> getAllByModules() {
		List<ModulePermissionDto> listModules = findPermissionsByRole(null);
		return listModules;
	}

	public PagedResult<AclPermissionDto> getAllPaginated(int page, int size) {
		PagedResult<AclPermissionDto> retObj = super.findAllPaginated(page, size, null);
		return retObj;
	}

	@Override
	public Optional<AclPermissionDto> findById(UUID id) {
		return Optional.empty();
	}

	@Cacheable(key = KEY_ROLE)
	public List<AclPermissionDto> findAllByRole(UUID roleId) {
		String sql = "SELECT * FROM acl_permissions WHERE role_id = ?";
		List<AclPermissionDto> list = jdbcTemplate.query(sql, new AclPermissionRowMapper(), roleId);
		return list;
	}

	@Override
	@Caching(evict = { @CacheEvict(key = KEY_ALL), @CacheEvict(key = KEY_ROLE) })
	public AclPermissionDto create(AclPermissionDto dto) {
		AclPermissionDto data = super.create(dto);
		audit.auditAsync(TABLE_NAME, data.getPermissionId(), EnumConstants.CREATE);
		return data;
	}

	@Override
	@Caching(evict = { @CacheEvict(key = KEY_ALL), @CacheEvict(key = KEY_ROLE) })
	public AclPermissionDto update(UUID id, AclPermissionDto dto) {
		AclPermissionDto data = super.update(id, dto);
		audit.auditAsync(TABLE_NAME, data.getPermissionId(), EnumConstants.UPDATE);
		return data;
	}

	@Caching(evict = { @CacheEvict(key = KEY_ALL), @CacheEvict(key = KEY_ROLE) })
	public AclPermissionDto partialUpdate(UUID id, Map<String, Object> dto) {
		UUID roleId = asUuid(dto.get("roleId"));
		Map<String, Object> compositeId = Map.of("role_id", roleId, "permission_id", id);
		AclPermissionDto data = super.patch(compositeId, dto);
		audit.auditAsync(TABLE_NAME, id, EnumConstants.PATCH);
		return data;
	}

	private UUID asUuid(Object value) {
		if (value instanceof UUID u)
			return u;
		if (value instanceof String s)
			return UUID.fromString(s);
		throw new IllegalArgumentException("Invalid UUID: " + value);
	}

	@CacheEvict(allEntries = true)
	public boolean delete(UUID id) {
		boolean flag = super.softDelete(id);
		audit.auditAsync(TABLE_NAME, id, EnumConstants.DELETE);
		return flag;
	}

	@CacheEvict(allEntries = true)
	public void clearAllCache() {
		log.info("Manually evicting all permission cache entries");
	}

	public List<AclPermissionDto> searchPage(String searchText, String status) {
		List<String> searchFields = List.of();
		List<AclPermissionDto> list = super.search(searchText, searchFields, status);
		return list;
	}

	// ------------------------------------------------------------
	// BULK INSERT (fast batch)
	// ------------------------------------------------------------
	@Caching(evict = { @CacheEvict(key = KEY_ALL), @CacheEvict(key = KEY_ROLE) })
	public boolean bulkInsert(UUID roleId, List<AclPermissionDto> permissions) {
		int[] rows = new int[0];
		List<AclPermissionDto> permissionsToProcess = permissions.stream()
				.filter(p -> p.isCanCreate() || p.isCanRead() || p.isCanUpdate() || p.isCanDelete())
				.collect(Collectors.toList());

		if (permissionsToProcess.isEmpty()) {
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

				ps.setObject(1, roleId); // role
				ps.setObject(2, p.getFeatureId()); // feature
				ps.setBoolean(3, p.isCanCreate());
				ps.setBoolean(4, p.isCanRead());
				ps.setBoolean(5, p.isCanUpdate());
				ps.setBoolean(6, p.isCanDelete());
				ps.setObject(7, userId); // created_by
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

		audit.auditAsync(TABLE_NAME, roleId, EnumConstants.CREATE_BULK);
		return rows.length > 0 ? true : false;
	}

	@Transactional("txManager")
	@Caching(evict = { @CacheEvict(key = KEY_ALL), @CacheEvict(key = KEY_ROLE) })
	public void bulkUpsertPermissions(UUID roleId, List<AclPermissionDto> permissions) {

		String sql = """
				INSERT INTO acl_permissions
				(role_id, feature_id, can_create, can_read, can_update, can_delete,
				 created_by, updated_by, status, created_at, updated_at)
				VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
				ON CONFLICT (role_id, feature_id)
				DO UPDATE SET
				    can_create = EXCLUDED.can_create,
				    can_read   = EXCLUDED.can_read,
				    can_update = EXCLUDED.can_update,
				    can_delete = EXCLUDED.can_delete,
				    updated_at = CURRENT_TIMESTAMP,
				    updated_by = EXCLUDED.updated_by
				""";

		UUID userId = AppUtil.getCurrentUserId();

		jdbcTemplate.batchUpdate(sql, permissions, permissions.size(), (ps, p) -> {

			// 🔒 Enforce rule: no READ → no other permissions
			boolean canRead = p.isCanRead();

			ps.setObject(1, roleId);
			ps.setObject(2, p.getFeatureId());
			ps.setBoolean(3, canRead && p.isCanCreate());
			ps.setBoolean(4, canRead);
			ps.setBoolean(5, canRead && p.isCanUpdate());
			ps.setBoolean(6, canRead && p.isCanDelete());
			ps.setObject(7, userId);
			ps.setObject(8, userId);
		});
	}

	// ------------------------------------------------------------
	// BULK UPDATE (fast batch)
	// ------------------------------------------------------------
	@Caching(evict = { @CacheEvict(key = KEY_ROLE), @CacheEvict(key = KEY_ALL) })
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
		audit.auditAsync(TABLE_NAME, roleId, EnumConstants.UPDATE_BULK);
	}

	@Cacheable(key = "#roleId == null ? 'acl:role:GENERIC' : 'acl:role:' + #roleId", unless = "#result.isEmpty()")
	public List<ModulePermissionDto> findPermissionsByRole(UUID roleId) {
		boolean isGeneric = (roleId == null);

		String sql = isGeneric ? PERMISSIONS_FOR_ALL : permissionbyRoleQuery;
		List<Map<String, Object>> rows = isGeneric ? jdbcTemplate.queryForList(sql)
				: jdbcTemplate.queryForList(sql, roleId, roleId);

		List<ModulePermissionDto> list = mapToModulePermissionDto(rows, !isGeneric);
		return list;
	}

	public boolean hasPermission(UUID roleId, String featureCode, PermissionAction action) {
		// Fetch cached permissions (DB hit only once per role)
		List<ModulePermissionDto> modules = findPermissionsByRole(roleId);
		if (modules == null || modules.isEmpty()) {
			return false;
		}

		for (ModulePermissionDto module : modules) {
			for (FeaturePermissionDto feature : module.features()) {
				if (!featureCode.equalsIgnoreCase(feature.featureName())) {
					continue;
				}

				return switch (action) {
				case CREATE -> feature.canCreate();
				case READ -> feature.canRead();
				case UPDATE -> feature.canUpdate();
				case DELETE -> feature.canDelete();
				};
			}
		}
		return false;
	}

	private List<ModulePermissionDto> mapToModulePermissionDto(List<Map<String, Object>> rows,
			boolean filterByPermission) {
		Map<UUID, ModulePermissionDto> moduleMap = new LinkedHashMap<>();

		for (Map<String, Object> row : rows) {
			UUID moduleId = (UUID) row.get("module_id");
			String moduleName = (String) row.get("module_name");

			FeaturePermissionDto feature = new FeaturePermissionDto((UUID) row.get("permission_id"),
					(UUID) row.get("role_id"), (String) row.get("role_name"), (Integer) row.get("role_level"),
					(String) row.get("finance_role"), (UUID) row.get("feature_id"), (String) row.get("feature_name"),
					(String) row.get("url"), (String) row.get("feature_type"),
					Boolean.TRUE.equals(row.get("can_create")), Boolean.TRUE.equals(row.get("can_read")),
					Boolean.TRUE.equals(row.get("can_update")), Boolean.TRUE.equals(row.get("can_delete")),
					(String) row.get("permission_status"));

			if (!filterByPermission || feature.canCreate() || feature.canRead() || feature.canUpdate()
					|| feature.canDelete()) {

				moduleMap.computeIfAbsent(moduleId, id -> new ModulePermissionDto(id, moduleName, new ArrayList<>()))
						.features().add(feature);
			}
		}

		return moduleMap.values().stream().filter(m -> !filterByPermission || !m.features().isEmpty()).toList();
	}

	private static final String permissionbyRoleQuery = """
			SELECT m.module_id, m.module_name, f.feature_id, f.feature_name, f.url, f.feature_type, r.role_id, r.role_name, r.role_level, r.finance_role, p.permission_id,
			    COALESCE(p.can_create, false) AS can_create,
			    COALESCE(p.can_read, false)   AS can_read,
			    COALESCE(p.can_update, false) AS can_update,
			    COALESCE(p.can_delete, false) AS can_delete,
			    COALESCE(p.status, 'INACTIVE') AS permission_status
			FROM modules m
			JOIN features f ON f.module_id = m.module_id AND f.status = 'ACTIVE'
			LEFT JOIN acl_permissions p ON p.feature_id = f.feature_id AND p.role_id = ?
			JOIN roles r ON r.role_id = ? AND r.status = 'ACTIVE'
			WHERE m.status = 'ACTIVE'
			ORDER BY m.module_name, f.feature_name
			""";

	// Query for ALL permissions (no role filtering)
	private static final String PERMISSIONS_FOR_ALL = """
			    SELECT m.module_id, m.module_name, f.feature_id, f.feature_name, f.url, f.feature_type, p.permission_id,
					    COALESCE(p.can_create, false) AS can_create,
					    COALESCE(p.can_read, false)   AS can_read,
					    COALESCE(p.can_update, false) AS can_update,
					    COALESCE(p.can_delete, false) AS can_delete,
					    COALESCE(p.status, 'INACTIVE') AS permission_status
					FROM modules m
					JOIN features f  ON f.module_id = m.module_id  AND f.status = 'ACTIVE'
					LEFT JOIN acl_permissions p
					    ON p.feature_id = f.feature_id
					    AND p.role_id IS NULL   -- 👈 GENERIC ACL
					WHERE  m.status = 'ACTIVE'
					ORDER BY m.module_name, f.feature_name
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
