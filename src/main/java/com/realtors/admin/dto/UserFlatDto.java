package com.realtors.admin.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserFlatDto {
	UUID userId;
	String fullName;
	UUID managerId;
	String email;
	String mobile;
	String employeeId;
}
