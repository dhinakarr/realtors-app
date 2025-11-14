package com.realtors.common.util;

import com.realtors.admin.dto.AclPermissionDto;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class AclPermissionRowMapper implements RowMapper<AclPermissionDto> {

    @Override
    public AclPermissionDto mapRow(ResultSet rs, int rowNum) throws SQLException {
        AclPermissionDto dto = new AclPermissionDto();

        dto.setPermissionId((UUID) rs.getObject("permission_id"));
        dto.setRoleId((UUID) rs.getObject("role_id"));
        dto.setFeatureId((UUID) rs.getObject("feature_id"));

        dto.setCanCreate(rs.getBoolean("can_create"));
        dto.setCanRead(rs.getBoolean("can_read"));
        dto.setCanUpdate(rs.getBoolean("can_update"));
        dto.setCanDelete(rs.getBoolean("can_delete"));
        dto.setStatus(rs.getString("status"));

        dto.setCreatedAt(rs.getTimestamp("created_at"));
        dto.setUpdatedAt(rs.getTimestamp("updated_at"));
        dto.setCreatedBy((UUID) rs.getObject("created_by"));
        dto.setUpdatedBy((UUID) rs.getObject("updated_by"));

        return dto;
    }
}
