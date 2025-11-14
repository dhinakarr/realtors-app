package com.realtors.admin.service;


import com.realtors.admin.dto.AclPermissionDto;
import com.realtors.admin.dto.FeaturePermissionDto;
import com.realtors.admin.dto.ModulePermissionDto;
import com.realtors.common.util.AclPermissionRowMapper;
import com.realtors.common.util.AppUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class AclPermissionService extends AbstractBaseService<AclPermissionDto, UUID> {
	
	private static final Logger log = LoggerFactory.getLogger(AclPermissionService.class);
	private static final String CACHE_NAME = "aclPermissions";
	private static final String TABLE_NAME = "acl_permissions";
    private static final String ID_COLUMNS = "role_id,permission_id";

    private final JdbcTemplate jdbcTemplate;

    public AclPermissionService(JdbcTemplate jdbcTemplate) {
    	super(AclPermissionDto.class, TABLE_NAME, jdbcTemplate);
        this.jdbcTemplate = jdbcTemplate;
    }

	@Override
	protected String getIdColumn() {
		return ID_COLUMNS;
	}
	
    @Override
    @Cacheable(value =CACHE_NAME, key = "'ALL'")
    public List<AclPermissionDto> findAll() {
        return super.findAll();
    }

    @Override
    @Cacheable(value =CACHE_NAME, key = "#id")
    public Optional<AclPermissionDto> findById(UUID id) {
    	log.info("Fetching permission from DB for id {}", id);
        return super.findById(id);
    }

    @Cacheable(value = CACHE_NAME, key = "'role:' + #roleId")
    public List<AclPermissionDto> findAllByRole(UUID roleId) {
        String sql = "SELECT * FROM acl_permissions WHERE role_id = ?";
        return jdbcTemplate.query(sql, new AclPermissionRowMapper(), roleId);
    }
    
    @Override
    @CachePut(value = CACHE_NAME, key = "#result.permissionId")
    @CacheEvict(value = CACHE_NAME, key = "'role:' + #dto['roleId']")
    public AclPermissionDto create(AclPermissionDto dto) {
        return super.create(dto);
    }

    @Override
    @CachePut(value = CACHE_NAME, key = "#id")
    @CacheEvict(value = CACHE_NAME, key = "'role:' + #dto['roleId']")
    public AclPermissionDto update(UUID id, AclPermissionDto dto) {
        return super.update(id, dto);
    }
    
    @CachePut(value = CACHE_NAME, key = "#id")
    @CacheEvict(value = CACHE_NAME, key = "'role:' + #dto['roleId']")
    public AclPermissionDto partialUpdate(UUID id, Map<String, Object> dto) {
        UUID roleId = asUuid(dto.get("roleId"));

        Map<String, Object> compositeId = Map.of(
            "role_id", roleId,
            "permission_id", id
        );
        return super.patch(compositeId, dto);
    }

    private UUID asUuid(Object value) {
        if (value instanceof UUID u) return u;
        if (value instanceof String s) return UUID.fromString(s);
        throw new IllegalArgumentException("Invalid UUID: " + value);
    }

    @CacheEvict(value = CACHE_NAME, allEntries = true)
    public boolean delete(UUID id) {
        return super.softDelete(id);
    }
    
    @CacheEvict(value = CACHE_NAME, key = "'ALL'")
    public void clearAllCache() {
        log.info("Manually evicting all permission cache entries");
    }
    
    public List<AclPermissionDto> searchPage(String searchText, String status) {
    	List<String> searchFields = List.of();
        return super.search(searchText, searchFields, status);
    }
    
    @Cacheable(value = CACHE_NAME, key = "'role:' + #roleId")
    public List<ModulePermissionDto> findPermissionsByRole(UUID roleId) {

    	List<Map<String, Object>> result = jdbcTemplate.queryForList(permissionbyRoleQuery, roleId);
        Map<UUID, ModulePermissionDto> moduleMap = new LinkedHashMap<>();

        for (Map<String, Object> row : result) {
        	UUID moduleId = (UUID) row.get("module_id");
            String moduleName = (String) row.get("module_name");

            FeaturePermissionDto feature = new FeaturePermissionDto(
                    (UUID) row.get("feature_id"),
                    (String) row.get("feature_name"),
                    (String) row.get("feature_type"),
                    (Boolean) row.get("can_create"),
                    (Boolean) row.get("can_read"),
                    (Boolean) row.get("can_update"),
                    (Boolean) row.get("can_delete")
            );

            moduleMap.computeIfAbsent(moduleId, id ->
                    new ModulePermissionDto(id, moduleName, new ArrayList<>())
            ).features().add(feature);
        }

        return new ArrayList<>(moduleMap.values());
    }
    
 // ------------------------------------------------------------
    //  BULK INSERT (fast batch)
    // ------------------------------------------------------------
    @CacheEvict(value = CACHE_NAME, key = "'role:' + #roleId")
    public void bulkInsert(UUID roleId, List<AclPermissionDto> permissions) {

        String sql = """
            INSERT INTO acl_permissions 
            (permission_id, role_id, feature_id, can_create, can_read, 
             can_update, can_delete, status, created_at, updated_at, 
             created_by, updated_by)
            VALUES (?,?,?,?,?,?,?,?,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,?,?)
        """;
        UUID userId = AppUtil.getCurrentUserId();
        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                AclPermissionDto p = permissions.get(i);

                ps.setObject(1, p.getPermissionId());
                ps.setObject(2, roleId);
                ps.setObject(3, p.getFeatureId());
                ps.setBoolean(4, p.isCanCreate());
                ps.setBoolean(5, p.isCanRead());
                ps.setBoolean(6, p.isCanUpdate());
                ps.setBoolean(7, p.isCanDelete());
                ps.setString(8, p.getStatus());
                ps.setObject(9, userId);
                ps.setObject(10, userId);
            }
            
            @Override
            public int getBatchSize() {
                return permissions.size();
            }
        });
    }
    
 // ------------------------------------------------------------
    //  BULK UPDATE (fast batch)
    // ------------------------------------------------------------
    @CacheEvict(value = CACHE_NAME, key = "'role:' + #roleId")
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
    }
    
    private static final String permissionbyRoleQuery = """
        SELECT
            m.module_id, m.module_name, f.feature_id, f.feature_name, f.feature_type,
            p.can_create, p.can_read, p.can_update, p.can_delete
        FROM acl_permissions p
        JOIN roles r ON r.role_id = p.role_id
        JOIN features f ON f.feature_id = p.feature_id
        JOIN modules m ON m.module_id = f.module_id
        WHERE p.status = 'ACTIVE' AND r.status = 'ACTIVE'
          AND f.status = 'ACTIVE' AND m.status = 'ACTIVE'
          AND r.role_id = ?
        ORDER BY m.module_name, f.feature_name
        """;
}
