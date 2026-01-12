package com.realtors.sales.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.realtors.sales.dto.SaleCommissionDTO;
import com.realtors.sales.finance.dto.CashFlowItemDTO;
import com.realtors.sales.finance.dto.PayableDetailsDTO;

public interface SaleCommissionRepository {
	SaleCommissionDTO insertCommission(SaleCommissionDTO dto);
    void updateCommission(UUID commissionId, BigDecimal percentage, BigDecimal amount);
    void updateStatus(UUID saleId, UUID userId, String status, boolean released);
    List<SaleCommissionDTO> findBySaleId(UUID saleId);
    public List<SaleCommissionDTO> findBySale(UUID saleId, UUID userId);
    public BigDecimal getTotalPayable();
    public BigDecimal getPaidBetween(LocalDateTime from, LocalDateTime to);
    public List<CashFlowItemDTO> getPayables(LocalDate from, LocalDate to);
    public BigDecimal getPaidThisMonth();
    public BigDecimal getTotalCommission(UUID saleId, UUID userId);
    public List<PayableDetailsDTO> getPayableDetails();
    public void handleCommissionReversal(UUID saleId);
    public void deleteCommissionData(UUID saleId, UUID userId);
}
