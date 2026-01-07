package com.realtors.dashboard.dto;

import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class UserPerformanceDTO {
    private UUID userId;
    private String userName;
    private double visitToSaleConversion;

    private List<SiteVisitDetailDTO> siteVisits;
    private List<SaleDetailDTO> sales;
    private List<ReceivableDetailDTO> receivable;
    private List<CommissionDetailsDTO> commission;

    // getters and setters
}
