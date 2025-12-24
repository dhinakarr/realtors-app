package com.realtors.dashboard.util;

import com.realtors.dashboard.dto.DashboardScope;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;

public final class DashboardHelper {

    private DashboardHelper() {}

    public static MapSqlParameterSource toParams(DashboardScope scope) {
        MapSqlParameterSource params = new MapSqlParameterSource();

        // Always present
        params.addValue("all", scope.isAll());
        params.addValue("userId", scope.getUserId());

        // Optional: projectIds
        if (!scope.isAll() && scope.getProjectIds() != null && !scope.getProjectIds().isEmpty()) {
            params.addValue("projectIds", scope.getProjectIds());
        }

        return params;
    }
}

