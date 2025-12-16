package com.realtors.sales.finance.dto;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.Data;

@Data
public class PayableDetailsDTO {
	UUID saleId;
	UUID projectId;
	UUID plotId;
	UUID agentId;
	String projectName;
	String agentName;
	String plotNumber;
	BigDecimal saleAmount;
	BigDecimal totalCommission;
	BigDecimal customerPaid;
	BigDecimal commissionPaid;
	BigDecimal commissionEligible;
	BigDecimal commissionPayable;
	String saleStatus;
}
