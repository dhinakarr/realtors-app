package com.realtors.dashboard.controller;

import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.realtors.common.ApiResponse;
import com.realtors.dashboard.dto.CommissionDetailsDTO;
import com.realtors.dashboard.dto.ReceivableDetailDTO;
import com.realtors.dashboard.service.FinanceService;
import com.realtors.sales.finance.dto.CashFlowItemDTO;
import com.realtors.sales.finance.dto.CashFlowStatus;
import com.realtors.sales.finance.dto.CashFlowType;
import com.realtors.sales.finance.dto.FinanceSummaryDTO;
import com.realtors.sales.finance.dto.PayableDetailsDTO;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/finance")
@RequiredArgsConstructor
public class FinanceController {

	private final FinanceService service;
	private static final Logger logger = LoggerFactory.getLogger(FinanceController.class);

	@GetMapping("/summary")
	public ResponseEntity<ApiResponse<FinanceSummaryDTO>> getSummary(
	        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
	        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
	) {
		
		logger.info("@FinanceController.getSummary FinanceSummaryDTO from:{}, to: {}", from, to);
	    return ResponseEntity.ok(
	        ApiResponse.success(
	            "Finance summary",
	            service.getSummary(from, to),
	            HttpStatus.OK
	        )
	    );
	}

	@GetMapping("/cashflow")
	public ResponseEntity<ApiResponse<List<CashFlowItemDTO>>> getCashFlow(
			@RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to,
			@RequestParam(required = false) CashFlowType type, @RequestParam(required = false) CashFlowStatus status) {
		return ResponseEntity
				.ok(ApiResponse.success("Cash flow", service.getCashFlow(from, to, type, status), HttpStatus.OK));
	}
	
	@GetMapping("/receivable/details")
	public ResponseEntity<ApiResponse<List<ReceivableDetailDTO>>> getReceivableDetails(
	        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
	        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
	) {
		List<ReceivableDetailDTO> list = service.getReceivableDetails(from, to);
	    return ResponseEntity.ok(
	        ApiResponse.success(
	            "Receivable Details",
	            list,
	            HttpStatus.OK
	        )
	    );
	}
	
	@GetMapping("/payable/details")
	public ResponseEntity<ApiResponse<List<PayableDetailsDTO>>> getPayableDetail(
			@RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate to) {
		List<PayableDetailsDTO> retValue = service.getPayableDetails(from, to);
		return ResponseEntity.ok(ApiResponse.success("Payable Details", retValue, HttpStatus.OK));
	}
	
	@GetMapping("/sale/details")
	public ResponseEntity<ApiResponse<List<ReceivableDetailDTO>>> getSaleDetail(@RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate to) {
		List<ReceivableDetailDTO> retValue = service.findSalesByStatus();
		return ResponseEntity.ok(ApiResponse.success("Sale Details", retValue, HttpStatus.OK));
	}
	
	@GetMapping("/received/details")
	public ResponseEntity<ApiResponse<List<ReceivableDetailDTO>>> getReceivedDetail(
			@RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate to) {
		List<ReceivableDetailDTO> retValue = service.getReceivedByThisMonth(from, to);
		return ResponseEntity.ok(ApiResponse.success("Received Details", retValue, HttpStatus.OK));
	}
	
	@GetMapping("/paid/details")
	public ResponseEntity<ApiResponse<List<CommissionDetailsDTO>>> getCommissionPaidDetail(@RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE) LocalDate to) {
		List<CommissionDetailsDTO> retValue = service.getCommissionsPaidThisMonth(from, to);
		return ResponseEntity.ok(ApiResponse.success("Received Details", retValue, HttpStatus.OK));
	}
}
