package com.realtors.admin.dto;


import java.util.List;
import java.util.UUID;

public record ModulePermissionDto(
        UUID moduleId,
        String moduleName,
        List<FeaturePermissionDto> features
) {}
