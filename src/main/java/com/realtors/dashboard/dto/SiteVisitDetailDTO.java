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
public class SiteVisitDetailDTO {
    private UUID siteVisitId;
    private LocalDate visitDate;
    private UUID projectId;
    private String projectName;
    private String agentName;
    private String managerName;
    private String customerName;
    private BigDecimal expenseAmount;
    private String status;
}
