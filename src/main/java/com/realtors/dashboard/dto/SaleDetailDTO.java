package com.realtors.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class SaleDetailDTO {
    private UUID saleId;
    private UUID projectId;
    private String projectName;
    private String customerName;
    private String agentName;
    private String plotNumber;
    private BigDecimal saleAmount;
    private BigDecimal baseAmount;
    private BigDecimal receivedAmount;
    private BigDecimal outstandingAmount;
    private String saleStatus;
    private LocalDate confirmedAt;
}
