package com.realtors.admin.dto;

import java.util.UUID;

public record FeaturePermissionDto(
		UUID permissionId,
		UUID roleId,
		String roleName,
		Integer roleLevel,
		boolean financeRole,
        UUID featureId,
        String featureName,
        String url,
        String featureType,
        boolean canCreate,
        boolean canRead,
        boolean canUpdate,
        boolean canDelete,
        String status
) {}

