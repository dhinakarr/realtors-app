package com.realtors.dashboard.dto;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class CommissionDetailDTO {
    private UUID saleId;
    private UUID userId;
    private BigDecimal commissionAmount;
    private BigDecimal commissionPaid;
    private BigDecimal commissionPayable;
}
