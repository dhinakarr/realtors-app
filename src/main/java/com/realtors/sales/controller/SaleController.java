package com.realtors.sales.controller;

import java.util.List;
import java.util.UUID;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.realtors.common.ApiResponse;
import com.realtors.common.util.AppUtil;
import com.realtors.dashboard.dto.UserPrincipalDto;
import com.realtors.projects.dto.PlotUnitDto;
import com.realtors.sales.dto.CancelRequest;
import com.realtors.sales.dto.PaymentDTO;
import com.realtors.sales.dto.SaleCommissionDTO;
import com.realtors.sales.dto.SaleCreateRequest;
import com.realtors.sales.dto.SaleDTO;
import com.realtors.sales.service.CommissionService;
import com.realtors.sales.service.PaymentService;
import com.realtors.sales.service.SaleService;
import com.realtors.sales.service.VisibilityService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
public class SaleController {

	private final SaleService saleService;
	private final CommissionService commissionService;
	private final PaymentService paymentService;
	private final VisibilityService visibilityService;
//	private static final Logger logger = LoggerFactory.getLogger(SaleController.class);

	@PostMapping
	public ResponseEntity<ApiResponse<SaleDTO>> createSale(@RequestBody SaleCreateRequest request) {
		SaleDTO sale = saleService.createSale(request);
		return ResponseEntity.ok(ApiResponse.success("Sale data created", sale, HttpStatus.CREATED));
	}

	@GetMapping("/{saleId}")
	public ResponseEntity<ApiResponse<SaleDTO>> getSale(@PathVariable UUID saleId) {
		SaleDTO sale = saleService.getSaleById(saleId);
		return ResponseEntity.ok(ApiResponse.success("Sale data created", sale, HttpStatus.OK));
	}

	@GetMapping
	public ResponseEntity<List<SaleDTO>> listSales(@RequestParam(required = false) UUID userId) {
		userId = (userId==null) ? AppUtil.getCurrentUserId() : userId;
 		List<SaleDTO> list = visibilityService.getVisibleSales(userId);
		return ResponseEntity.ok(list);
	}

	@GetMapping("/{saleId}/commissions")
	public ResponseEntity<List<SaleCommissionDTO>> getCommissions(@PathVariable UUID saleId) {
		return ResponseEntity.ok(commissionService.getCommissionsBySale(saleId));
	}

	@PostMapping("/{saleId}/payments")
	public ResponseEntity<PaymentDTO> addPayment(@PathVariable UUID saleId, @RequestBody PaymentDTO paymentRequest) {

		paymentRequest.setSaleId(saleId);
		PaymentDTO result = paymentService.addPayment(paymentRequest);

		return ResponseEntity.ok(result);
	}

	@GetMapping("/{saleId}/payments")
	public ResponseEntity<List<PaymentDTO>> listPayments(@PathVariable UUID saleId) {
		return ResponseEntity.ok(paymentService.getPaymentsBySale(saleId));
	}

	@PostMapping("/{saleId}/confirm")
	public ResponseEntity<String> confirmSale(@PathVariable UUID saleId) {
		saleService.confirmSale(saleId);
		return ResponseEntity.ok("Sale confirmed successfully");
	}

	@PostMapping("/{plotId}/cancel")
	public ResponseEntity<ApiResponse<PlotUnitDto>> cancelSale(@PathVariable UUID plotId, @RequestBody CancelRequest request) {
	    PlotUnitDto dto = saleService.cancelBooking(plotId, request);
	    return ResponseEntity.ok(ApiResponse.success("Booking cancelled successfully", dto));
	}
}
