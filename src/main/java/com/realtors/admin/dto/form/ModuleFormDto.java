package com.realtors.admin.dto.form;

import java.util.List;
import java.util.UUID;

public record ModuleFormDto(
	    UUID moduleId,
	    String moduleName,
	    List<FeatureFormDto> features
	) {}
