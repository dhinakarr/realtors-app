package com.realtors.admin.service;

import com.realtors.admin.dto.RoleDto;
import com.realtors.admin.dto.RoleHierarchyDto;

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
    private static final Logger logger = LoggerFactory.getLogger(RoleService.class);

    public RoleService(JdbcTemplate jdbcTemplate) {
    	super(RoleDto.class, "roles", jdbcTemplate);
        this.jdbcTemplate = jdbcTemplate;
        logger.info("Role Service");
    }
    
	@Override
	protected String getIdColumn() {
		return "role_id";
	}
	
	public List<RoleDto> getAllRoles() {
		return super.findAll();
	}
	
	public Optional<RoleDto> getRoleById(UUID id) {
		return super.findById(id);
	}
	
	public RoleDto createRole(RoleDto dto) {
		return super.create(dto);
	}
	
	public RoleDto updateRoleById(UUID id, RoleDto dto) {
		return super.update(id, dto);
	}
	
	public RoleDto patchRoleUpdate(UUID id, Map<String, Object> dto) {
		return super.patch(id, dto);
	}
	
	public boolean deleteRole(UUID id) {
		return super.softDelete(id);
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
