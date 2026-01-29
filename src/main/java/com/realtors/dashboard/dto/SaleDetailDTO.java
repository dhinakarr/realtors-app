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
public class SaleDetailDTO {
    private UUID saleId;
    private UUID projectId;
    private String projectName;
    private UUID customerId;
    private String customerName;
    private UUID agentId;
    private String agentName;
    private UUID plotId;
    private String plotNumber;
    private BigDecimal saleAmount;
    private BigDecimal baseAmount;
    private BigDecimal receivedAmount;
    private BigDecimal outstandingAmount;
    private String saleStatus;
    private LocalDate confirmedAt;
}
