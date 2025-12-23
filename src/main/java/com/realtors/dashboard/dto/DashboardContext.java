package com.realtors.dashboard.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class DashboardContext {
    private UUID userId;
    private UserRole role;   // ADMIN, AGENT, FINANCE, MANAGER
}

