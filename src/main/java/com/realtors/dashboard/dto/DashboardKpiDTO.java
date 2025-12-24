package com.realtors.dashboard.dto;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardKpiDTO {

    private int totalPlots;
    private int availablePlots;
    private int bookedPlots;

    private BigDecimal totalSales;
    private BigDecimal totalReceived;
    private BigDecimal totalOutstanding;

    private BigDecimal totalCommissionPayable;

    private long totalSiteVisits;
    private BigDecimal avgConversionRatio;
}
