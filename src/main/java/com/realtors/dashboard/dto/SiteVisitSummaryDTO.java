package com.realtors.dashboard.dto;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
@AllArgsConstructor
public class SiteVisitSummaryDTO {
    private UUID agentId;
    private String agentName;
    private long totalVisits;
    private long totalCustomers;
    private long conversions;
    private BigDecimal conversionRatio;
}
