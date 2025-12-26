package com.realtors.dashboard.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class DashboardKPI {
	private Long leadsCreated;
    private Long followUpsDue;
    private Long siteVisitsScheduled;
    private Long bookingsDone;
    private BigDecimal commissionApproved;
    private BigDecimal commissionPending;
}
