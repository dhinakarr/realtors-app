package com.realtors.admin.dto;

import java.util.UUID;

public record ListUserDto(
		UUID userId,
		String fullName,
		String mobile,
		String email,
		String employeeId,
		UUID managerId,
		String managerName,
		UUID roleId,
		String roleName,
		String status
		) {

}
