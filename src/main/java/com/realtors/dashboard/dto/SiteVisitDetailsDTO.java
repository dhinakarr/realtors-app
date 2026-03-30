package com.realtors.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class SiteVisitDetailsDTO {
    private UUID siteVisitId;
    private LocalDate visitDate;
    private UUID projectId;
    private String projectName;
    private UUID agantId;
    private String agentName;
    private UUID customerId;
    private String customerName;
    private boolean isConverted;
    private UUID saleId;
    private BigDecimal salePrice;
    private LocalDate confirmedAt;
}
