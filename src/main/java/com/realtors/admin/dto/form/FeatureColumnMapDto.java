package com.realtors.admin.dto.form;

import java.util.Map;

public record FeatureColumnMapDto(String[] displayColumns, 
											Map<String, String> fieldMap,
											String rowKeyField,
											boolean paginated,
											String viewType	
											) {}
