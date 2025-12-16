package com.realtors.sales.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.realtors.sales.dto.SaleDTO;
import com.realtors.sales.finance.dto.CashFlowItemDTO;
import com.realtors.sales.finance.dto.CashFlowStatus;

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
    void updateSaleStatus(UUID saleId, String status);
    public BigDecimal getTotalAmount(UUID saleId);
    SaleDTO findSaleByPlotId(UUID plotId);
    public BigDecimal getOutstandingDueToday();
    public List<CashFlowItemDTO> findReceivables(
			LocalDate from,
			LocalDate to
	) ;
    public BigDecimal getTotalSalesAmount();
    public List<CashFlowItemDTO> findReceivables(
	        LocalDate from,
	        LocalDate to,
	        CashFlowStatus status
	);
}


