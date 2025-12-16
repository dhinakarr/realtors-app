package com.realtors.sales.dto;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SalePaymentSummaryDTO {

	private BigDecimal totalSaleAmount;
	private BigDecimal totalReceived;
	private BigDecimal outstandingAmount;
	private BigDecimal commissionPaid;
}
