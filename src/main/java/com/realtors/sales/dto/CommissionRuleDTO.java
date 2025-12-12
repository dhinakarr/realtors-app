package com.realtors.sales.dto;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.Data;

@Data
public class CommissionRuleDTO {
    private UUID ruleId;
    private UUID projectId;
    private UUID roleId;
    private BigDecimal percentage; // 40.00 etc
    private String roleName;
}

