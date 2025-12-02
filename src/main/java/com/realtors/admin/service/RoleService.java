package com.realtors.admin.service;

import com.realtors.admin.dto.PagedResult;
import com.realtors.admin.dto.RoleDto;
import com.realtors.admin.dto.RoleHierarchyDto;
import com.realtors.admin.dto.form.DynamicFormResponseDto;
import com.realtors.admin.dto.form.EditResponseDto;
import com.realtors.common.EnumConstants;
import com.realtors.common.service.AuditContext;
import com.realtors.common.service.AuditTrailService;
import com.realtors.common.util.AppUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class RoleService extends AbstractBaseService<RoleDto, UUID> {

    private final JdbcTemplate jdbcTemplate;
    private final AuditTrailService audit;
    private static final Logger logger = LoggerFactory.getLogger(RoleService.class);
    
//    List<LookupDefinition> lookupDefs = List.of(new LookupDefinition("roles", "roles", "role_id", "role_name"));

    public RoleService(JdbcTemplate jdbcTemplate, AuditTrailService audit) {
    	super(RoleDto.class, "roles", jdbcTemplate);
        this.jdbcTemplate = jdbcTemplate;
        this.audit = audit;
        addDependentLookup("parent_role_id", "roles", "role_id", "role_name", "managerRole");
//        logger.info("Role Service");
    }
    
	@Override
	protected String getIdColumn() {
		return "role_id";
	}
	
	/** ✅ User form response */
    public DynamicFormResponseDto getRolesFormData() {
    	audit.auditAsync("roles", null, EnumConstants.FORM.toString(), 
    			AppUtil.getCurrentUserId(), AuditContext.getIpAddress(), AuditContext.getUserAgent());
    	return super.buildDynamicFormResponse();
    }
    
    public EditResponseDto<RoleDto> editRolesResponse(UUID roleId) {
        Optional<RoleDto> opt = super.findById(roleId);
        DynamicFormResponseDto form = super.buildDynamicFormResponse();
        
        audit.auditAsync("roles", null, EnumConstants.EDIT_FORM.toString(), 
    			AppUtil.getCurrentUserId(), AuditContext.getIpAddress(), AuditContext.getUserAgent());
        
        return opt.map(user -> new EditResponseDto<>(user, form))
                  .orElse(null);
    }
	
	public List<RoleDto> getAllRoles() {
		List<RoleDto> list = super.findAll();
		audit.auditAsync("roles", list.getFirst().getRoleId(), EnumConstants.GET_ALL.toString(), 
    			AppUtil.getCurrentUserId(), AuditContext.getIpAddress(), AuditContext.getUserAgent());
		return list;
	}
	
	public List<RoleDto> searchRoles(String searchText) {
		audit.auditAsync("roles", null, EnumConstants.SEARCH.toString(), 
    			AppUtil.getCurrentUserId(), AuditContext.getIpAddress(), AuditContext.getUserAgent());
		return super.search(searchText, List.of("role_name", "description"), null);
	}
	
	public PagedResult<RoleDto> getPaginatedData(int page, int size) {
		/*
		 * return new FeatureListResponseDto<>("Roles", "table", List.of("Role Name",
		 * "Description", "Status"), Map.ofEntries(Map.entry("Role Name", "roleName"),
		 * Map.entry("Description", "description"), Map.entry("Status", "status")),
		 * "roleId", true, // pagination enabled super.findAllPaginated(page, size,
		 * null), // <-- MUST return PagedResult<AppUserDto>
		 * super.getLookupData(lookupDefs) // <-- fully dynamic lookup map );
		 */
		PagedResult<RoleDto> paged = super.findAllPaginated(page, size, null);
		audit.auditAsync("roles", paged.data().getFirst().getRoleId(), EnumConstants.PAGED.toString(), 
    			AppUtil.getCurrentUserId(), AuditContext.getIpAddress(), AuditContext.getUserAgent());
		return paged;
	}
	
	public Optional<RoleDto> getRoleById(UUID id) {
		Optional<RoleDto> dto = super.findById(id);
		audit.auditAsync("roles", dto.isPresent() ? dto.get().getRoleId() : null, EnumConstants.BY_ID.toString(), 
    			AppUtil.getCurrentUserId(), AuditContext.getIpAddress(), AuditContext.getUserAgent());
		return super.findById(id);
	}
	
	public RoleDto createRole(RoleDto dto) {
		RoleDto data = super.create(dto);
		audit.auditAsync("roles", data.getRoleId() , EnumConstants.CREATE.toString(), 
    			AppUtil.getCurrentUserId(), AuditContext.getIpAddress(), AuditContext.getUserAgent());
		return data;
	}
	
	public RoleDto updateRoleById(UUID id, RoleDto dto) {
		RoleDto data = super.update(id, dto);
		audit.auditAsync("roles", data.getRoleId() , EnumConstants.UPDATE.toString(), 
    			AppUtil.getCurrentUserId(), AuditContext.getIpAddress(), AuditContext.getUserAgent());
		return data;
	}
	
	public RoleDto patchRoleUpdate(UUID id, Map<String, Object> dto) {
		RoleDto data = super.patch(id, dto);
		audit.auditAsync("roles", data.getRoleId() , EnumConstants.UPDATE.toString(), 
				AppUtil.getCurrentUserId(), AuditContext.getIpAddress(), AuditContext.getUserAgent());
		return data;
	}
	
	public boolean deleteRole(UUID id) {
		audit.auditAsync("roles", id , EnumConstants.DELETE.toString(), 
				AppUtil.getCurrentUserId(), AuditContext.getIpAddress(), AuditContext.getUserAgent());
		return super.softDelete(id);
	}
	
	public int computeRoleLevel(UUID parentRoleId) {
	    if (parentRoleId == null) {
	        return 0;
	    }

	    String sql = """
	        WITH RECURSIVE r AS (SELECT id, parent_role_id, 0 AS lvl
	            FROM roles WHERE id = ?
	            UNION ALL
	            SELECT p.id, p.parent_role_id, r.lvl + 1
	            FROM roles p
	            JOIN r ON r.parent_role_id = p.id
	        )
	        SELECT lvl FROM r ORDER BY lvl DESC LIMIT 1
	        """;

	    return jdbcTemplate.queryForObject(sql, Integer.class, parentRoleId);
	}
	
	public List<RoleHierarchyDto> findSubRoles(UUID rootRoleId) {
        String sql = """
            WITH RECURSIVE sub_roles AS (
                SELECT r.role_id, r.role_name, r.description, r.parent_role_id
                FROM roles r
                WHERE r.role_id = ?
                UNION ALL
                SELECT r.role_id, r.role_name, r.description, r.parent_role_id
                FROM roles r
                INNER JOIN sub_roles sr ON r.parent_role_id = sr.role_id
            )
            SELECT s.role_id, s.role_name, s.description, s.parent_role_id, p.role_name AS parent_role_name
            FROM sub_roles s
            LEFT JOIN roles p ON s.parent_role_id = p.role_id
            ORDER BY s.role_name
            """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new RoleHierarchyDto(
                UUID.fromString(rs.getString("role_id")),
                rs.getString("role_name"),
                rs.getString("description"),
                rs.getString("parent_role_id") != null ? UUID.fromString(rs.getString("parent_role_id")) : null,
                rs.getString("parent_role_name")
        ), rootRoleId);
    }

    public List<RoleHierarchyDto> findParentChain(UUID roleId) {
        String sql = """
            WITH RECURSIVE parent_chain AS (
                SELECT r.role_id, r.role_name, r.description, r.parent_role_id
                FROM roles r
                WHERE r.role_id = ?
                UNION ALL
                SELECT r.role_id, r.role_name, r.description, r.parent_role_id
                FROM roles r
                INNER JOIN parent_chain pc ON pc.parent_role_id = r.role_id
            )
            SELECT c.role_id, c.role_name, c.description, c.parent_role_id, p.role_name AS parent_role_name
            FROM parent_chain c
            LEFT JOIN roles p ON c.parent_role_id = p.role_id
            """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new RoleHierarchyDto(
                UUID.fromString(rs.getString("role_id")),
                rs.getString("role_name"),
                rs.getString("description"),
                rs.getString("parent_role_id") != null ? UUID.fromString(rs.getString("parent_role_id")) : null,
                rs.getString("parent_role_name")
        ), roleId);
    }
}
