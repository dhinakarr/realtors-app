package com.realtors.sales.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.realtors.common.util.AppUtil;
import com.realtors.sales.dto.PaymentDTO;
import com.realtors.sales.dto.SaleDTO;
import com.realtors.sales.finance.dto.CashFlowItemDTO;
import com.realtors.sales.finance.dto.CashFlowStatus;
import com.realtors.sales.finance.dto.ReceivableDetailsDTO;
import com.realtors.sales.repository.PaymentRepositoryImpl;
import com.realtors.sales.repository.SaleRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PaymentService {

	private final PaymentRepositoryImpl paymentRepo;
	private final SaleRepository salestRepo;
	private final SaleLifecycleService saleLifecycleService;
	private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

	public PaymentDTO addPayment(PaymentDTO dto) {
	    PaymentDTO saved = paymentRepo.save(dto);
	    saleLifecycleService.recalculate(saved.getSaleId());
	    return saved;
	}
	
	public BigDecimal getTotalOutstanding() {
	    BigDecimal totalSales = salestRepo.getTotalSalesAmount();
	    BigDecimal received = paymentRepo.getTotalReceivedAll();
	    return totalSales.subtract(received);
	}
	
	public BigDecimal getTotalReceivable() {
		return paymentRepo.getTotalReceivables();
	}
	
	public List<PaymentDTO> getPaymentsBySale(UUID saleId) {
		return paymentRepo.findBySaleId(saleId);
	}
	
	public PaymentDTO getPaymentsById(UUID paymentId) {
		return paymentRepo.getById(paymentId);
	}

	public PaymentDTO save(PaymentDTO dto) {
		UUID actorUserId = AppUtil.getCurrentUserId();
		SaleDTO sale = salestRepo.findSaleByPlotId(dto.getPlotId());
		dto.setSaleId(sale.getSaleId());
		if (dto.getPaymentDate() == null) {
			dto.setPaymentDate(LocalDateTime.now());
		}

		// RECEIVED payment → collected_by is required
		if (dto.getPaymentType().equals("RECEIVED")) {
			dto.setCollectedBy(actorUserId);
			dto.setPaidTo(null);
		}

		// PAID payment → paid_to is required
		if (dto.getPaymentType().equals("PAID")) {
			dto.setPaidTo(dto.getPaidTo());
			dto.setCollectedBy(null);
		}
		validatePayment(dto, sale);
		paymentRepo.save(dto);
		saleLifecycleService.recalculate(sale.getSaleId());
		return dto;
	}

	public PaymentDTO updatePayment(UUID paymentId, PaymentDTO dto) {
		PaymentDTO existing = paymentRepo.getById(paymentId);

		if (existing.isVerified()) {
			throw new IllegalStateException("Verified payments cannot be edited");
		}

		dto.setPaymentId(paymentId);
		paymentRepo.update(dto);
		saleLifecycleService.evaluateSaleStatus(existing.getSaleId());
		return dto;
	}


	public BigDecimal getTotalReceived(UUID saleId) {
		return paymentRepo.getTotalReceived(saleId);
	}
	
	public BigDecimal getTotalSaleAmount(UUID saleId) {
		return salestRepo.getTotalAmount(saleId);
	}

	public BigDecimal getOutstanding(UUID saleId) {
		BigDecimal totalSale = getTotalSaleAmount(saleId);
		BigDecimal totalReceived = getTotalReceived(saleId);
		return totalSale.subtract(totalReceived);
	}

	public BigDecimal getCommissionPaid(UUID saleId) {
		return paymentRepo.getTotalPaid(saleId);
	}
	
	public BigDecimal getPaidThisMonth() {
		return paymentRepo.getTotalPaidThisMonth();
	}
	
	public BigDecimal getPaidTotal() {
		return paymentRepo.getTotalPaid();
	}
	
	
	public BigDecimal getReceivedThisMonth() {
		LocalDateTime from = LocalDate.now().withDayOfMonth(1).atStartOfDay();
		LocalDateTime to   = LocalDateTime.now();
		return paymentRepo.getReceivedBetween(from, to);
	}
	
	public BigDecimal getExpectedToday() {
		return salestRepo.getOutstandingDueToday();
	}
	
	public List<ReceivableDetailsDTO> getReceivableDetails() {
		return paymentRepo.getReceivableDetails();
	}

	public List<CashFlowItemDTO> getReceivables(
			LocalDate from,
			LocalDate to,
			CashFlowStatus status
	) {
		List<CashFlowItemDTO> list = salestRepo.findReceivables(from, to);

		if (status == null) return list;

		return list.stream()
				.filter(i -> i.getStatus() == status)
				.toList();
	}
	
	public void verifyPayment(UUID paymentId) {
	    PaymentDTO payment = paymentRepo.verify(paymentId, AppUtil.getCurrentUserId());
	    saleLifecycleService.recalculate(payment.getSaleId());
	}
	
	public void deletePayment(UUID paymentId) {
	    PaymentDTO payment = paymentRepo.getById(paymentId);
	    if (payment.isVerified()) {
			throw new IllegalStateException("Verified payments cannot be deleted");
		}
	    paymentRepo.softDelete(paymentId, AppUtil.getCurrentUserId());
	    saleLifecycleService.recalculate(payment.getSaleId());
	}
	
	private void validatePayment(PaymentDTO dto, SaleDTO sale) {
		if (dto.getAmount() == null || dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
			throw new IllegalArgumentException("Payment amount must be positive");
		}

		if (dto.getPaymentType() == null) {
			throw new IllegalArgumentException("Payment type is required");
		}

		if (dto.getPaymentType().equals("RECEIVED")) {
			BigDecimal outstanding = getOutstandingAmount(sale.getSaleId());
			if (dto.getAmount().compareTo(outstanding) > 0) {
				throw new IllegalArgumentException("Received amount exceeds outstanding balance");
			}
		}
	}
	
	public BigDecimal getOutstandingAmount(UUID saleId) {
	    BigDecimal totalSale = salestRepo.getTotalAmount(saleId);
	    BigDecimal received  = paymentRepo.getTotalReceived(saleId);
	    return totalSale.subtract(received);
	}

}
