package com.realtors.sales.dto;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.Data;

@Data
public class SaleCreateRequest {
    private UUID plotId;
    private UUID projectId;
    private UUID customerId;
    private UUID soldBy;  // salesperson user_id
    
    // Optional override (usually service calculates price)
    private BigDecimal extraCharges;
    private BigDecimal area;
    private BigDecimal advanceAmount;   
}

