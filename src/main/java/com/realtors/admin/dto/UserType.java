package com.realtors.admin.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;

@JsonFormat(shape = JsonFormat.Shape.STRING)
public enum UserType {
    INTERNAL,
    CUSTOMER;

    @JsonCreator
    public static UserType from(String value) {
        return UserType.valueOf(value.toUpperCase());
    }
}