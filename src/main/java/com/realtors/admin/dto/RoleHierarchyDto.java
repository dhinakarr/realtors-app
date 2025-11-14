package com.realtors.admin.dto;

import java.util.UUID;

public record RoleHierarchyDto(
        UUID roleId,
        String roleName,
        String description,
        UUID parentRoleId,
        String parentRoleName
) {}

