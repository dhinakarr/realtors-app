package com.realtors.dashboard.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class DashboardResponse {

    // Cards
    private BigDecimal totalOutstanding;
    private BigDecimal totalCommissionPayable;

    // Inventory
    private List<InventoryDetailDTO> inventoryStats;

    // Tables
    private List<ReceivableDetailDTO> receivables;
    private List<CommissionDetailsDTO> commissionPayables;

    // Optional flags
    private boolean inventoryVisible;
    private boolean receivableVisible;
    private boolean commissionVisible;

    // getters & setters
}
