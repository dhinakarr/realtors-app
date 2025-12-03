package com.realtors.admin.dto;

import java.sql.Timestamp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserAuthDto {
	private int authId;
	private java.util.UUID userId;
	private String username;
	private String passwordHash;
    private Timestamp lastLogin;
    private int failedAttempts;
    private boolean isLocked;
	private String userType;
	private boolean forcePasswordChange;
}
