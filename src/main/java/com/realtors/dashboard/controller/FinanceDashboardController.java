package com.realtors.dashboard.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.realtors.common.ApiResponse;
import com.realtors.dashboard.service.FinanceDashboardService;
import com.realtors.sales.finance.dto.CashFlowItemDTO;
import com.realtors.sales.finance.dto.CashFlowStatus;
import com.realtors.sales.finance.dto.CashFlowType;
import com.realtors.sales.finance.dto.FinanceSummaryDTO;
import com.realtors.sales.finance.dto.PayableDetailsDTO;
import com.realtors.sales.finance.dto.ReceivableDetailsDTO;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/finance")
@RequiredArgsConstructor
public class FinanceDashboardController {

	private final FinanceDashboardService service;

	@GetMapping("/summary")
	public ResponseEntity<ApiResponse<FinanceSummaryDTO>> getSummary() {
		return ResponseEntity.ok(ApiResponse.success("Finance summary", service.getSummary(), HttpStatus.OK));
	}

	@GetMapping("/cashflow")
	public ResponseEntity<ApiResponse<List<CashFlowItemDTO>>> getCashFlow(
			@RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to,
			@RequestParam(required = false) CashFlowType type, @RequestParam(required = false) CashFlowStatus status) {
		return ResponseEntity
				.ok(ApiResponse.success("Cash flow", service.getCashFlow(from, to, type, status), HttpStatus.OK));
	}
	
	@GetMapping("/receivable/details")
	public ResponseEntity<ApiResponse<List<ReceivableDetailsDTO>>> getDetail() {
		List<ReceivableDetailsDTO> retValue = service.getReceivableDetails();
		return ResponseEntity.ok(ApiResponse.success("Cash flow", retValue, HttpStatus.OK));
	}
	
	@GetMapping("/payable/details")
	public ResponseEntity<ApiResponse<List<PayableDetailsDTO>>> getPayableDetail() {
		List<PayableDetailsDTO> retValue = service.getPayableDetails();
		return ResponseEntity.ok(ApiResponse.success("Cash flow", retValue, HttpStatus.OK));
	}
}
