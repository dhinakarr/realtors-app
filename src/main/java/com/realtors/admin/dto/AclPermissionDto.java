package com.realtors.admin.dto;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AclPermissionDto {
	private UUID permissionId;
	private UUID roleId;
	private String roleName;
	private UUID featureId;
	private String featureName;
	private boolean canCreate;
	private boolean canRead;
	private boolean canUpdate;
	private boolean canDelete;
	private String status;
	private @JsonIgnore Timestamp createdAt;
	private @JsonIgnore Timestamp updatedAt;
	private @JsonIgnore UUID createdBy;
	private @JsonIgnore UUID updatedBy;
}

