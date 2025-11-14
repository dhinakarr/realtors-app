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
public class RoleDto {
        private UUID roleId;
        private String roleName;
        private String description;
        private  Timestamp createdAt;
        private  Timestamp updatedAt;
        private String status;
        private @JsonIgnore UUID createdBy;
        private @JsonIgnore UUID updatedBy;
        private UUID parentRoleId;
}

