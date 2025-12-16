package com.realtors.sales.finance.dto;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FinanceSummaryDTO {
	private BigDecimal totalReceivable;
	private BigDecimal receivedThisMonth;
	private BigDecimal expectedToday;

	private BigDecimal commissionPayable;
	private BigDecimal commissionPaidThisMonth;
}
