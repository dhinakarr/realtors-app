package com.realtors.sales.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CommissionRulePatchDTO {

	@NotNull
	private UUID ruleId;

	private UUID roleId;
	private UUID userId;

	private CommissionType commissionType; // enum
	private BigDecimal commissionValue;

	private Integer priority;
	private Boolean active;

	private LocalDate effectiveFrom;
	private LocalDate effectiveTo;

	@NotNull
	private UUID updatedBy;
}
