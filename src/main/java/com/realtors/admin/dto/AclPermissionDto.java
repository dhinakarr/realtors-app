package com.realtors.admin.dto;

import java.util.UUID;

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
	private UUID featureId;
	private boolean canCreate;
	private boolean canRead;
	private boolean canUpdate;
	private boolean canDelete;
	private String status;
	private Timestamp createdAt;
	private Timestamp updatedAt;
	private UUID createdBy;
	private UUID updatedBy;
}

