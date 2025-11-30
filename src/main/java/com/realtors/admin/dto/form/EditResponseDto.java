package com.realtors.admin.dto.form;

public record EditResponseDto<T>(T data, DynamicFormResponseDto form) {}
