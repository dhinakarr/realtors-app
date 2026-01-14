package com.realtors.sales.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import lombok.Data;

@Data
public class PaymentRuleDetailsDto {
	private UUID ruleId;

    // Scope
    private UUID projectId;

    // Rule applies to ONE of these
    private UUID roleId;     // nullable
    private UUID userId;     // nullable
    private String roleName;
    private String userName;
    // Calculation
    private String commissionType;  
    // PERCENTAGE | PER_SQFT | FLAT

    private BigDecimal commissionValue;  
    // 40.00 (%)
    // 150.00 (₹/sqft)
    // 25000 (flat)

    // Control
    private Integer priority;      // user > role
    private Boolean active;

    // Accounting / Audit
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private UUID createdBy;
    private UUID updatedBy;


}
