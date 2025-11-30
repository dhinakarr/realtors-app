package com.realtors.admin.dto;

import java.sql.Timestamp;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

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
	private @JsonIgnore Timestamp createdAt;
	private @JsonIgnore Timestamp updatedAt;
	private String status;
	private @JsonIgnore UUID createdBy;
	private @JsonIgnore UUID updatedBy;
}


