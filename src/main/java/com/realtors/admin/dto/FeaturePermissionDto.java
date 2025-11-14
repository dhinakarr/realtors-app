package com.realtors.admin.dto;

import java.util.UUID;

public record FeaturePermissionDto(
        UUID featureId,
        String featureName,
        String featureType,
        boolean canCreate,
        boolean canRead,
        boolean canUpdate,
        boolean canDelete
) {}

