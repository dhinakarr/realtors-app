package com.realtors.dashboard.dto;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
@AllArgsConstructor
public class InventoryStatusDTO {

    private UUID plotId;
    private UUID projectId;
    private String projectName;
    private String plotNumber;
    private String inventoryStatus;
    private BigDecimal area;
    private BigDecimal basePrice;
    private BigDecimal totalPrice;
    // getters & setters
}
