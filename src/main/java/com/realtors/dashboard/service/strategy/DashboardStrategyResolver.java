package com.realtors.dashboard.service.strategy;

import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class DashboardStrategyResolver {

    private final Map<String, DashboardStrategy> strategies;

    public DashboardStrategyResolver(Map<String, DashboardStrategy> strategies) {
        this.strategies = strategies;
    }

    public DashboardStrategy resolve(String role) {
        return strategies.getOrDefault(
            role,
            strategies.get("AGENT") // fallback
        );
    }
}
