package com.realtors.dashboard.dto;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class FinancialSummaryDTO {
    private UUID projectId;
    private String projectName;
    private BigDecimal totalSales;
    private BigDecimal totalReceived;
    private BigDecimal totalOutstanding;
}
