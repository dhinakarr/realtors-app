package com.realtors.admin.dto;

import java.sql.Timestamp;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModuleDto {
	private UUID moduleId;
	private String moduleName;
	private String description;
	private Timestamp createdAt;
	private Timestamp updatedAt;
	private String status;
	private UUID createdBy;
	private UUID updatedBy;
}


