package com.realtors.dashboard.dto;

public enum UserRole {

    PA,
    PM,
    PH,
    MD,
    HR,
    FINANCE,
    CUSTOMER;

    public static UserRole from(String role) {
        return UserRole.valueOf(role.toUpperCase());
    }
}
