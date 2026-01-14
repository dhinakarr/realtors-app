package com.realtors.admin.dto;

import java.util.UUID;

import lombok.Data;

@Data
public class UserMiniDto {
	UUID userId;
	String fullName;
}
