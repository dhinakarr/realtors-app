package com.realtors.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.sql.Timestamp;
import java.util.Map;
import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppUserDto {
    private UUID userId;
    @NotNull(message = "roleId is required")
    private UUID roleId;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String roleName;
    @NotBlank(message = "email is required")
    @Email(message = "invalid email")
    private String email;
    @Size(max = 15)
    private String mobile;
    private String passwordHash;
    @Size(max = 55)
    private String fullName;
    private String address; 
    String branchCode;
    String employeeId;
    String hierarchyCode;
    int seqNo;
    private @JsonIgnore Timestamp lastLogin;
    private @JsonIgnore Timestamp createdAt;
    private @JsonIgnore Timestamp updatedAt;
    private String status;
    private Map<String, Object> meta;
    private UUID managerId;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String managerName;
    private byte[] profileImage;
    private @JsonIgnore UUID createdBy;
    private @JsonIgnore UUID updatedBy;
}
