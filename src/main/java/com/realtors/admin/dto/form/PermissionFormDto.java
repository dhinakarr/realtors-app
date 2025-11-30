package com.realtors.admin.dto.form;

import java.util.List;

public record PermissionFormDto(
	    List<RoleFormDto> roles,
	    List<ModuleFormDto> modules
	) {}
