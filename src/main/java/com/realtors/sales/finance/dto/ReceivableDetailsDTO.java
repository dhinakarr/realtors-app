package com.realtors.sales.finance.dto;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.Data;

@Data
public class ReceivableDetailsDTO {
	UUID projectId;
	String projectName;
	UUID saleId;
	UUID plotId;
	String plotNumber;
	UUID customerId;
	String customerName;
	UUID agentId;
	String agentName;
	BigDecimal saleAmount;
	BigDecimal totalReceived;
	BigDecimal outstandingAmount;
}
