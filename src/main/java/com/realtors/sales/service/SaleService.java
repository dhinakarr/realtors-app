package com.realtors.sales.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.realtors.admin.service.UserAuthService;
import com.realtors.common.util.AppUtil;
import com.realtors.customers.dto.CustomerDto;
import com.realtors.customers.service.CustomerService;
import com.realtors.projects.dto.PlotUnitDto;
import com.realtors.projects.dto.ProjectDto;
import com.realtors.projects.repository.PlotUnitRepository;
import com.realtors.projects.services.ProjectService;
import com.realtors.sales.dto.PlotStatus;
import com.realtors.sales.dto.SaleCreateRequest;
import com.realtors.sales.dto.SaleDTO;
import com.realtors.sales.dto.SalesStatus;
import com.realtors.sales.repository.SaleRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SaleService {

	private final PlotUnitRepository plotRepository;
	private final ProjectService projectService;
	private final SaleRepository saleRepository;
	private final CommissionService commissionService;
	private final CustomerService customerService;
	private final UserAuthService authService;

	public SaleDTO getSaleById(UUID saleId) {
		return saleRepository.findById(saleId);
	}

	public SaleDTO getSaleByPlotId(UUID plotId) {
		return saleRepository.findById(plotId);
	}

	@Transactional("txManager")
	public SaleDTO createSale(SaleCreateRequest request) {
		// 1. Fetch plot
		PlotUnitDto plot = plotRepository.findByPlotId(request.getPlotId());
		// 2. Fetch project
		ProjectDto project = projectService.findById(plot.getProjectId()).orElse(null);
		BigDecimal area = plot.getArea();
		// 3. Calculate base price
		BigDecimal basePrice = area.multiply(project.getPricePerSqft());
		// 4. Determine extra charges
		BigDecimal extraCharges = request.getExtraCharges();
		if (extraCharges == null)
			extraCharges = calculateExtraCharges(project);

		BigDecimal totalPrice = basePrice.add(extraCharges);
		UUID userId = request.getSoldBy() == null ? AppUtil.getCurrentUserId() : request.getSoldBy();

		// 5. Create Sale in DB
		SaleDTO sale = saleRepository.createSale(request.getPlotId(), plot.getProjectId(), request.getCustomerId(),
				userId, area, basePrice, extraCharges, totalPrice);

		plot.setStatus(SalesStatus.BOOKED.toString());
		plot.setCustomerId(request.getCustomerId());
		plotRepository.update(plot);

		// insert customer into user_auth table to enable login access to customer
		CustomerDto customerDto = customerService.getCustomer(request.getCustomerId());
		authService.createUserAuth(customerDto.getCustomerId(), customerDto.getEmail(), "Test@123", customerDto.getRoleId(), "CUSTOMER");

		// 6. Distribute commission after sale creation
		commissionService.distributeCommission(sale);

		return sale;
	}

	public void confirmSale(UUID saleId) {
		SaleDTO sale = saleRepository.findById(saleId);
		// Update sale status
		saleRepository.updateSaleStatus(saleId, "CONFIRMED");
		// Optionally recalc commission (if dynamic)
		commissionService.distributeCommission(sale);
	}

	public BigDecimal getTotalAmount(UUID saleId) {
		return saleRepository.getTotalAmount(saleId);
	}

	@Transactional
	public void cancelSale(UUID saleId) {
		SaleDTO sale = saleRepository.findById(saleId);
		saleRepository.updateSaleStatus(saleId, SalesStatus.CANCELLED.name());
		plotRepository.updatePlotStatus(sale.getPlotId(), PlotStatus.CANCELLED.name());
	}

	private BigDecimal calculateExtraCharges(ProjectDto project) {
		return sum(project.getRegCharges(), project.getDocCharges(), project.getOtherCharges());
	}

	private static BigDecimal sum(BigDecimal... values) {
		BigDecimal result = BigDecimal.ZERO;
		for (BigDecimal v : values) {
			if (v != null)
				result = result.add(v);
		}
		return result;
	}
}
