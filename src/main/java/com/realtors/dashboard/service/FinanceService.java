package com.realtors.dashboard.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.realtors.sales.finance.dto.CashFlowItemDTO;
import com.realtors.sales.finance.dto.CashFlowStatus;
import com.realtors.sales.finance.dto.CashFlowType;
import com.realtors.sales.finance.dto.FinanceSummaryDTO;
import com.realtors.sales.finance.dto.PayableDetailsDTO;
import com.realtors.sales.finance.dto.ReceivableDetailsDTO;
import com.realtors.sales.service.CommissionService;
import com.realtors.sales.service.PaymentService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FinanceService {

	private final PaymentService paymentService;
	private final CommissionService commissionService;

	public FinanceSummaryDTO getSummary(UUID saleId) {

		return FinanceSummaryDTO.builder().totalReceivable(paymentService.getOutstanding(saleId))
				.receivedThisMonth(paymentService.getReceivedThisMonth())
				.expectedToday(paymentService.getExpectedToday()).commissionPayable(commissionService.getTotalPayable())
				.commissionPaidThisMonth(commissionService.getPaidThisMonth()).build();
	}

	public FinanceSummaryDTO getSummary() {
		BigDecimal totalReceivable      = nz(paymentService.getTotalReceivable());
	    BigDecimal receivedThisMonth    = nz(paymentService.getReceivedThisMonth());
	    BigDecimal expectedToday        = nz(paymentService.getExpectedToday());
	    BigDecimal totalPayable         = nz(commissionService.getTotalPayable());
	    BigDecimal paidTotal            = nz(paymentService.getPaidTotal());
	    BigDecimal paidThisMonth        = nz(paymentService.getPaidThisMonth());

	    return FinanceSummaryDTO.builder()
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

	public List<ReceivableDetailsDTO> getReceivableDetails() {
		return paymentService.getReceivableDetails();
	}

	public List<PayableDetailsDTO> getPayableDetails() {
		return commissionService.getPayableDetails();
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
