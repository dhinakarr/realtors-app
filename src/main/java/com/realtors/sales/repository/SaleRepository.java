package com.realtors.sales.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.realtors.sales.dto.SaleDTO;

public interface SaleRepository {
    SaleDTO createSale(
            UUID plotId,
            UUID projectId,
            UUID customerId,
            UUID soldBy,
            BigDecimal area,
            BigDecimal basePrice,
            BigDecimal extraCharges,
            BigDecimal totalPrice);
    SaleDTO findById(UUID saleId);
    void updateSaleStatus(UUID saleId, String status, LocalDateTime confirmedAt);
}

