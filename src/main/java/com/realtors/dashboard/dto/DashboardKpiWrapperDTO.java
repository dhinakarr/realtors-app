package com.realtors.dashboard.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardKpiWrapperDTO {
	private DashboardKPI today;
    private DashboardKPI month;
    private DashboardKPI total;
}
