package com.realtors.sales.dto;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CommissionDistributionDTO {
    private UUID roleId;
    private UUID userId;
    private String roleName;
    private BigDecimal amount;
    private int level;
}
