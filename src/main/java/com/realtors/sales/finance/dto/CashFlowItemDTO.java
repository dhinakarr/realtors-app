package com.realtors.sales.finance.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import lombok.Data;

@Data
public class CashFlowItemDTO {

	private CashFlowType type; // RECEIVABLE, PAYABLE

	private UUID saleId;
	private String plotNo;

	private String partyName; // Customer or Agent

	private BigDecimal amount;
	private LocalDate dueDate;

	private CashFlowStatus status; 
	// DUE, OVERDUE, PAID, APPROVED

	private UUID referenceId; 
	// paymentId or commissionId
}
