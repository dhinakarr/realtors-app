package com.realtors.sales.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.realtors.admin.service.UserAuthService;
import com.realtors.common.util.AppUtil;
import com.realtors.customers.dto.CustomerDto;
import com.realtors.customers.service.CustomerService;
import com.realtors.dashboard.dto.ReceivableDetailDTO;
import com.realtors.projects.dto.PlotUnitDto;
import com.realtors.projects.dto.ProjectDto;
import com.realtors.projects.repository.PlotUnitRepository;
import com.realtors.projects.services.PlotUnitService;
import com.realtors.projects.services.ProjectService;
import com.realtors.sales.dto.CancelRequest;
import com.realtors.sales.dto.PaymentType;
import com.realtors.sales.dto.PlotStatus;
import com.realtors.sales.dto.SaleCreateRequest;
import com.realtors.sales.dto.SaleDTO;
import com.realtors.sales.dto.SalesStatus;
import com.realtors.sales.repository.PaymentRepositoryImpl;
import com.realtors.sales.repository.SaleRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SaleService {

	private final PlotUnitRepository plotRepository;
	private final ProjectService projectService;
	private final PlotUnitService plotService;
	private final SaleRepository saleRepository;
	private final CommissionService commissionService;
	private final CustomerService customerService;
	private final UserAuthService authService;
	private final CommissionPaymentService comPayService;
	private final PaymentRepositoryImpl paymentRepo;
	private final CommissionDistributionService commisionDistributionService;
	
	private static final Logger logger = LoggerFactory.getLogger(SaleService.class);

	public SaleDTO getSaleById(UUID saleId) {
		return saleRepository.findById(saleId);
	}

	public SaleDTO getSaleByPlotId(UUID plotId) {
		return saleRepository.findById(plotId);
	}
	
	public List<ReceivableDetailDTO> findSalesByStatus(List<String> status) {
		return saleRepository.findSalesByStatus(status);
	}

	@Transactional("txManager")
	public SaleDTO createSale(SaleCreateRequest request) {
		// 1. Fetch plot
		PlotUnitDto plot = plotRepository.findByPlotId(request.getPlotId());
		// 2. Fetch project
		ProjectDto project = projectService.findById(plot.getProjectId()).orElse(null);
		BigDecimal area = plot.getArea();
		boolean isPrime = plot.getIsPrime();
		BigDecimal perSqft = AppUtil.nz(project.getPricePerSqft());

		if (isPrime)
			perSqft = plot.getRatePerSqft();

		// 3. Calculate base price
		BigDecimal basePrice = area.multiply(perSqft);

		// 4. Determine extra charges
		BigDecimal stampDuty = AppUtil.percent(project.getRegCharges());
		BigDecimal guideline = area.multiply(AppUtil.nz(project.getGuidanceValue()));
		BigDecimal registrationCharge = guideline.multiply(stampDuty).setScale(2, RoundingMode.HALF_UP);
		
		project.setRegCharges(registrationCharge);
		BigDecimal extraCharges = calculateExtraCharges(project);
		BigDecimal totalPrice = basePrice.add(AppUtil.nz(extraCharges));

		UUID userId = request.getSoldBy() == null ? AppUtil.getCurrentUserId() : request.getSoldBy();
		// 5. Create Sale in DB
		SaleDTO sale = saleRepository.createSale(request.getPlotId(), plot.getProjectId(), request.getCustomerId(),
				userId, area, basePrice, extraCharges, totalPrice);

		plot.setStatus(SalesStatus.BOOKED.toString());
		plot.setCustomerId(request.getCustomerId());
		plotRepository.update(plot);

		// insert customer into user_auth table to enable login access to customer
		CustomerDto customerDto = customerService.getCustomer(request.getCustomerId());
		if (!authService.isUserPresent(customerDto.getCustomerId())) {
			authService.createUserAuth(customerDto.getCustomerId(), customerDto.getEmail(), "Test@123",
					customerDto.getRoleId(), "CUSTOMER");
		}
		// 6. Distribute commission after sale creation
//		comPayService.distributeCommission(sale.getSaleId(), area);
		commisionDistributionService.distributeCommission(project.getProjectId(), userId, basePrice, area, sale.getSaleId());
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
	
	public BigDecimal getTotalSaleAmount() {
		return saleRepository.getTotalSalesAmount();
	}

	@Transactional
	public void cancelSale(UUID saleId) {
		SaleDTO sale = saleRepository.findById(saleId);
		saleRepository.updateSaleStatus(saleId, SalesStatus.CANCELLED.name());
		plotRepository.updatePlotStatus(sale.getPlotId(), PlotStatus.CANCELLED.name());
	}

	private BigDecimal calculateExtraCharges(ProjectDto project) {
		return sum(AppUtil.nz(project.getRegCharges()), AppUtil.nz(project.getDocCharges()),
				AppUtil.nz(project.getOtherCharges()));
	}

	private static BigDecimal sum(BigDecimal... values) {
		BigDecimal result = BigDecimal.ZERO;
		for (BigDecimal v : values) {
			if (v != null)
				result = result.add(AppUtil.nz(v));
		}
		return result;
	}
	
	@Transactional("txManager")
	public PlotUnitDto cancelBooking(UUID plotId, CancelRequest request) {
		SaleDTO sale = saleRepository.findSaleByPlotId(plotId);
		
		commissionService.reversePayment(sale.getSaleId(), null, SalesStatus.CANCELLED.name());
		paymentRepo.paymentReversed(sale.getSaleId(), PaymentType.REVERSED.name());
		saleRepository.updateSaleStatus(sale.getSaleId(), SalesStatus.CANCELLED.name());
		
		Map<String, Object> partialData = new HashMap<>();
		partialData.put("remarks", request.getReason());
		partialData.put("status", SalesStatus.AVAILABLE.name());
		PlotUnitDto dto = plotService.updateCancel(plotId, partialData);
		
		return dto;
	}
	
	
}
