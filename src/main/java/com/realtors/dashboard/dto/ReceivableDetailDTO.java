package com.realtors.dashboard.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class ReceivableDetailDTO {

    private UUID saleId;
    private UUID projectId;
    private String projectName;

    private UUID plotId;
    private String plotNumber;

    private UUID customerId;
    private String customerName;

    private UUID agentId;
    private String agentName;

    private BigDecimal saleAmount;
    private BigDecimal totalReceived;
    private BigDecimal outstandingAmount;

    private LocalDateTime confirmedAt;

    // getters & setters
}

