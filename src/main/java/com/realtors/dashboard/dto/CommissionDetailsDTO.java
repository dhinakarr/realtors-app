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
public class CommissionDetailsDTO {

    private UUID commissionId;
    private UUID saleId;

    private UUID agentId;
    private String agentName;

    private UUID projectId;
    private String projectName;

    private UUID plotId;
    private String plotNumber;

    private BigDecimal saleAmount;
    private BigDecimal customerPaid;

    private BigDecimal totalCommission;
    private BigDecimal commissionPaid;
    private BigDecimal commissionEligible;
    private BigDecimal commissionPayable;

    private String saleStatus;
    private LocalDateTime confirmedAt;

    // getters & setters
}
