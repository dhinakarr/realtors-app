package com.realtors.sales.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.realtors.common.util.AppUtil;
import com.realtors.projects.dto.PlotUnitDto;
import com.realtors.projects.repository.PlotUnitRepository;
import com.realtors.sales.dto.PaymentType;
import com.realtors.sales.dto.PlotStatus;
import com.realtors.sales.dto.SaleDTO;
import com.realtors.sales.dto.SalePaymentSummaryDTO;
import com.realtors.sales.dto.SalesStatus;
import com.realtors.sales.repository.PaymentRepositoryImpl;
import com.realtors.sales.repository.SaleCommissionRepository;
import com.realtors.sales.repository.SaleRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SaleLifecycleService {

	private final SaleRepository saleRepo;
	private final PlotUnitRepository plotRepo;
	private final PaymentRepositoryImpl paymentRepo;
	private final SaleCommissionRepository commissionRepo;

	@Transactional("txManager")
	public void evaluateSaleStatus(UUID saleId) {
		SaleDTO sale = saleRepo.findById(saleId);
		PlotUnitDto plotDto = plotRepo.findByPlotId(sale.getPlotId());

		BigDecimal totalAmount = sale.getTotalPrice();
		BigDecimal received = paymentRepo.getTotalReceived(saleId);

		SalesStatus newSaleStatus = determineSaleStatus(totalAmount, received);
		PlotStatus newPlotStatus = determinePlotStatus(newSaleStatus);

		// Update only if changed
		if (!sale.getSaleStatus().equals(newSaleStatus.name())) {
			saleRepo.updateSaleStatus(saleId, newSaleStatus.name());
		}

		if (!plotDto.getStatus().equals(newPlotStatus.name())) {
			plotRepo.updatePlotStatus(sale.getPlotId(), newPlotStatus.name());
		}
	}

	public SalePaymentSummaryDTO getPaymentSummary(UUID saleId) {
		
		BigDecimal totalSales = saleRepo.getTotalAmount(saleId);
	    BigDecimal received = paymentRepo.getTotalReceived(saleId);
	     totalSales.subtract(AppUtil.nz(received));

		return SalePaymentSummaryDTO.builder().
				totalSaleAmount(totalSales)
				.totalReceived(received)
				.outstandingAmount( totalSales.subtract(AppUtil.nz(received)))
				.commissionPaid(paymentRepo.getTotalPaid(saleId)).build();
	}

	@Transactional("txManager")
	public void recalculate(UUID saleId, String paymentType) {
		SaleDTO sale = saleRepo.findById(saleId);

		BigDecimal totalSaleAmount = saleRepo.getTotalAmount(saleId);
		BigDecimal totalReceived = paymentRepo.getTotalReceived(saleId);
		BigDecimal totalCommission = commissionRepo.getTotalCommission(saleId, sale.getSoldBy());
		BigDecimal totalPaid = paymentRepo.getTotalPaid(saleId);

		// No payments yet
		if (totalReceived.compareTo(BigDecimal.ZERO) == 0) {
			markInitiated(sale);
			return;
		}
		
		if (paymentType.equalsIgnoreCase(PaymentType.PAID.name())) {
			// Fully Paid
			if (totalPaid.compareTo(totalCommission) == 0) {
				commissionRepo.updateStatus(saleId, sale.getSoldBy(), PaymentType.FULLY_PAID.name(), true);
				return;
			} 
			
			// Partially Paid Commission
			if (totalPaid.compareTo(totalCommission) <= 0) {
				commissionRepo.updateStatus(saleId, sale.getSoldBy(), PaymentType.PARTIALLY_PAID.name(), false);
				return;
			}
		}
		
		// Fully Received
		if (totalReceived.compareTo(totalSaleAmount) >= 0) {
			markCompleted(sale);
			return;
		}
		// Partial payment
		markInProgress(sale);
	}

	private SalesStatus determineSaleStatus(BigDecimal total, BigDecimal received) {
		if (received == null || received.compareTo(BigDecimal.ZERO) == 0) {
			return SalesStatus.INITIATED;
		}
		if (received.compareTo(total) < 0) {
			return SalesStatus.PARTIALLY_PAID;
		}
		return SalesStatus.COMPLETED;
	}

	private PlotStatus determinePlotStatus(SalesStatus saleStatus) {
		switch (saleStatus) {
		case COMPLETED:
			return PlotStatus.SOLD;
		case CANCELLED:
			return PlotStatus.AVAILABLE;
		default:
			return PlotStatus.BOOKED;
		}
	}

	/*
	 * ===================== STATE TRANSITIONS =====================
	 */

	private void markInitiated(SaleDTO sale) {
		saleRepo.updateSaleStatus(sale.getSaleId(), SalesStatus.INITIATED.name());
		plotRepo.updatePlotStatus(sale.getPlotId(), PlotStatus.BOOKED.name());
	}

	private void markInProgress(SaleDTO sale) {
		saleRepo.updateSaleStatus(sale.getSaleId(), SalesStatus.IN_PROGRESS.name());
		plotRepo.updatePlotStatus(sale.getPlotId(), PlotStatus.BOOKED.name());
	}

	private void markCompleted(SaleDTO sale) {
		saleRepo.updateSaleStatus(sale.getSaleId(), SalesStatus.COMPLETED.name());
		plotRepo.updatePlotStatus(sale.getPlotId(), PlotStatus.SOLD.name());
	}

	public void cancelSale(UUID saleId) {
		SaleDTO sale = saleRepo.findById(saleId);

		saleRepo.updateSaleStatus(saleId, SalesStatus.CANCELLED.name());
		plotRepo.updatePlotStatus(sale.getPlotId(), PlotStatus.CANCELLED.name());
	}

}
