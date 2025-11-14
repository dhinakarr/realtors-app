package com.realtors.admin.dto;

import java.util.List;
import java.util.Map;

public record LoginResponse (
	Map<String, Object> token,
	List<ModulePermissionDto> permission
) {}
