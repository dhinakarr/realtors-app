package com.realtors.admin.dto.form;

import com.realtors.admin.dto.AppUserDto;

public record UserEditResponseDto (AppUserDto user, DynamicFormResponseDto form) {
}
