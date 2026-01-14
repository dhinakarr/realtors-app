package com.realtors.sales.dto;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.Data;

@Data
public class CommissionSpreadRuleDTO {

	private UUID ruleId;
    private UUID roleId;
    private UUID userId;      
    private String roleName;
    private int roleLevel;

    private String commissionType; // PERCENTAGE | AMOUNT_PER_SQFT | FLAT
    private BigDecimal commissionValue;
}
