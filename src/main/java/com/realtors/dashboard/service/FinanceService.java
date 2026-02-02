package com.realtors.dashboard.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.realtors.dashboard.dto.CommissionDetailsDTO;
import com.realtors.dashboard.dto.ReceivableDetailDTO;
import com.realtors.sales.finance.dto.CashFlowItemDTO;
import com.realtors.sales.finance.dto.CashFlowStatus;
import com.realtors.sales.finance.dto.CashFlowType;
import com.realtors.sales.finance.dto.FinanceSummaryDTO;
import com.realtors.sales.finance.dto.PayableDetailsDTO;
import com.realtors.sales.service.CommissionService;
import com.realtors.sales.service.PaymentService;
import com.realtors.sales.service.SaleService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FinanceService {

	private final PaymentService paymentService;
	private final CommissionService commissionService;
	private final SaleService saleService;
	private static final Logger logger = LoggerFactory.getLogger(FinanceService.class);

	public FinanceSummaryDTO getSummary(UUID saleId) {

		return FinanceSummaryDTO.builder().totalReceivable(paymentService.getOutstanding(saleId))
				.receivedThisMonth(paymentService.getReceivedThisMonth())
				.expectedToday(paymentService.getExpectedToday()).commissionPayable(commissionService.getTotalPayable())
				.commissionPaidThisMonth(commissionService.getPaidThisMonth()).build();
	}

	public FinanceSummaryDTO getSummary(LocalDate from, LocalDate to) {
		BigDecimal totalSalesAmount = nz(saleService.getTotalSaleAmount(from, to));
		BigDecimal totalReceivable      = nz(paymentService.getTotalReceivable(from, to));
	    BigDecimal receivedThisMonth    = nz(paymentService.getReceivedThisMonth(from, to));
	    BigDecimal expectedToday        = nz(paymentService.getExpectedToday());
	    BigDecimal totalPayable         = nz(commissionService.getTotalPayable(from, to));
	    BigDecimal paidTotal            = nz(paymentService.getPaidTotal(from, to));
	    BigDecimal paidThisMonth        = nz(paymentService.getPaidThisMonth(from, to));

	    return FinanceSummaryDTO.builder()
	    	.totalSaleAmount(totalSalesAmount)
	        .totalReceivable(totalReceivable)
	        .receivedThisMonth(receivedThisMonth)
	        .expectedToday(expectedToday)
	        .commissionPayable(totalPayable.subtract(paidTotal))
	        .commissionPaidThisMonth(paidThisMonth)
	        .build();
	}

	private BigDecimal nz(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value;
	}

	public List<ReceivableDetailDTO> getReceivableDetails(LocalDate from, LocalDate to) {
		List<ReceivableDetailDTO> list = paymentService.getReceivableDetails(from, to);
		return list;
	}
	
	public List<ReceivableDetailDTO> getReceivedByThisMonth(LocalDate from, LocalDate to) {
		List<ReceivableDetailDTO> list = paymentService.getReceivedDetails(from, to);
		return list;
	}
	
	public List<ReceivableDetailDTO> findSalesByStatus() {
		return saleService.findSalesByStatus(List.of("BOOKED", "IN_PROGRESS"));
	}

	public List<PayableDetailsDTO> getPayableDetails(LocalDate from, LocalDate to) {
		List<PayableDetailsDTO> list = commissionService.getPayableDetails(from, to);
		return list;
	}
	
	public List<CommissionDetailsDTO> getCommissionsPaidThisMonth(LocalDate from, LocalDate to) {
		return commissionService.getCommissionsPaid(from, to);
	}

	public List<CashFlowItemDTO> getCashFlow(LocalDate from, LocalDate to, CashFlowType type, CashFlowStatus status) {
		List<CashFlowItemDTO> items = new ArrayList<>();
		if (type == null || type == CashFlowType.RECEIVABLE) {
			items.addAll(paymentService.getReceivables(from, to, status));
		}
		if (type == null || type == CashFlowType.PAYABLE) {
			items.addAll(commissionService.getPayables(from, to, status));
		}
		return items.stream().sorted(Comparator.comparing(CashFlowItemDTO::getAmount).reversed()).toList();
	}
}
