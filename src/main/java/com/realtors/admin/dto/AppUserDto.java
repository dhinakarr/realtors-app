package com.realtors.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;
import java.util.Map;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppUserDto {
    private UUID userId;
    private UUID roleId;
    private String roleName;
    private String email;
    private String mobile;
    private String passwordHash;
    private String fullName;
    private Timestamp lastLogin;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private String status;
    private Map<String, Object> meta;
    private UUID managerId;
    private UUID createdBy;
    private UUID updatedBy;
}
