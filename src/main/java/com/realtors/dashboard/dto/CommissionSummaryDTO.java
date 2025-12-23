package com.realtors.dashboard.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class CommissionSummaryDTO {
    private BigDecimal totalCommission;
    private BigDecimal totalPaid;
    private BigDecimal totalPayable;
}
