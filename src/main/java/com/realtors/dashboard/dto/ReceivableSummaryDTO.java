package com.realtors.dashboard.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class ReceivableSummaryDTO {
    private BigDecimal totalSaleAmount;
    private BigDecimal totalReceived;
    private BigDecimal totalOutstanding;
}
