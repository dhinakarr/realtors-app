package com.realtors.dashboard.service.strategy;

import com.realtors.dashboard.dto.DashboardContext;
import com.realtors.dashboard.dto.DashboardResponse;

public interface DashboardStrategy {

    DashboardResponse buildDashboard(DashboardContext context);
}
