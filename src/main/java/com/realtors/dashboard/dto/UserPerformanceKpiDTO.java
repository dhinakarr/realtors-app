package com.realtors.dashboard.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class UserPerformanceKpiDTO {
	
	private int totalVisits;
	private int totalSales;
	private BigDecimal totalReceived;
	private BigDecimal totalCommission;

}
