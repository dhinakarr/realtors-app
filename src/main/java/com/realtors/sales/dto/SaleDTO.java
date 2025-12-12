package com.realtors.sales.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SaleDTO {
    private UUID saleId;
    private UUID plotId;
    private UUID projectId;
    private UUID customerId;
    private UUID soldBy;
    private BigDecimal area;
    private BigDecimal basePrice;
    private BigDecimal extraCharges;
    private BigDecimal totalPrice;
    private String saleStatus;
    private LocalDateTime confirmedAt;
    
    public SaleDTO(UUID saleId, UUID projectId, UUID plotId,  UUID customerId, BigDecimal area, BigDecimal totalPrice, LocalDateTime confirmedAt) {
    	this.totalPrice = totalPrice;
    	this.area = area;
    	this.saleId = saleId;
    	this.projectId = projectId;
    	this.customerId = customerId;
    	this.confirmedAt = confirmedAt;
    }
}
