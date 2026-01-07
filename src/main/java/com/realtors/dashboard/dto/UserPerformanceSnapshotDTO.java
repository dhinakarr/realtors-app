package com.realtors.dashboard.dto;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class UserPerformanceSnapshotDTO {

    private UUID userId;
    private String fullName;

    private UUID managerId;
    private String managerName;

    // Site visit
    private long totalSiteVisits;
    private long totalCustomers;

    // Sales
    private long totalSales;
    private BigDecimal salesValue;
    private BigDecimal totalReceived;
    private BigDecimal totalOutstanding;

    // Commission
    private BigDecimal commissionEarned;
    private BigDecimal commissionPaid;
    private BigDecimal commissionPayable;

    // Derived (service-calculated)
    private BigDecimal conversionRatio;
}
