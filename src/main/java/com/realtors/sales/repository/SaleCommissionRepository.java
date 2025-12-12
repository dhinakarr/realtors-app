package com.realtors.sales.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.realtors.sales.dto.SaleCommissionDTO;

public interface SaleCommissionRepository {
	SaleCommissionDTO insertCommission(SaleCommissionDTO dto);
    void updateCommission(UUID commissionId, BigDecimal percentage, BigDecimal amount);
    List<SaleCommissionDTO> findBySaleId(UUID saleId);
    public List<SaleCommissionDTO> findBySale(UUID saleId, UUID userId);
}
