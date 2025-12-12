package com.realtors.sales.dto;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaleCommissionDTO {
    private UUID commissionId;
    private UUID saleId;
    private UUID userId;
    private UUID roleId;
    private BigDecimal percentage;
    private BigDecimal commissionAmount;
    private Timestamp createdAt;
    
    public SaleCommissionDTO(UUID saleId, UUID userId, UUID roleId, BigDecimal percentage, BigDecimal commissionAmount) {
    	this.saleId = saleId;
    	this.userId = userId;
    	this.roleId = roleId;
    	this.percentage = percentage;
    	this.commissionAmount = commissionAmount;
    }
}

