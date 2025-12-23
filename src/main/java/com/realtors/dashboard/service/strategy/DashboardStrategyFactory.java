package com.realtors.dashboard.service.strategy;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.realtors.dashboard.dto.UserRole;

@Component
public class DashboardStrategyFactory {

    private final Map<UserRole, DashboardStrategy> strategyMap;

    public DashboardStrategyFactory(List<DashboardStrategy> strategies) {

        this.strategyMap = new EnumMap<>(UserRole.class);

        strategies.forEach(strategy -> {
            RoleStrategy annotation =
                strategy.getClass().getAnnotation(RoleStrategy.class);

            if (annotation != null) {
                strategyMap.put(annotation.value(), strategy);
            }
        });
    }

    public DashboardStrategy getStrategy(UserRole role) {
        return strategyMap.get(role);
    }
}
