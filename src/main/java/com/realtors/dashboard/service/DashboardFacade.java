package com.realtors.dashboard.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

import com.realtors.dashboard.dto.DashboardContext;
import com.realtors.dashboard.dto.DashboardResponse;
import com.realtors.dashboard.dto.UserRole;
import com.realtors.dashboard.service.strategy.DashboardStrategyResolver;

@Service
public class DashboardFacade {

    private final DashboardStrategyResolver resolver;

    public DashboardFacade(DashboardStrategyResolver resolver) {
        this.resolver = resolver;
    }

    public DashboardResponse getDashboard(UUID userId, UserRole role) {
        DashboardContext context = new DashboardContext(userId, role);
        return resolver
            .resolve(role.name())
            .buildDashboard(context);
    }
}
