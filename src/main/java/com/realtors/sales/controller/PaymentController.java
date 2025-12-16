package com.realtors.sales.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.realtors.common.ApiResponse;
import com.realtors.sales.dto.PaymentDTO;
import com.realtors.sales.service.PaymentService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

	private final PaymentService service;

	@PostMapping
	public ResponseEntity<ApiResponse<PaymentDTO>> createPayment(@RequestBody PaymentDTO dto) {
		PaymentDTO saved = service.save(dto);
		return ResponseEntity.ok(ApiResponse.success("Payment saved", saved, HttpStatus.OK));
	}
	
	@GetMapping("/{paymentId}")
	public ResponseEntity<ApiResponse<PaymentDTO>> getById(@PathVariable UUID paymentId) {
		return ResponseEntity.ok(
			ApiResponse.success("Payment fetched", service.getPaymentsById(paymentId), HttpStatus.OK)
		);
	}
	
	@GetMapping("/sale/{saleId}/list")
	public ResponseEntity<ApiResponse<List<PaymentDTO>>> getPaymentsBySale(@PathVariable UUID saleId) {
		return ResponseEntity.ok(
			ApiResponse.success("Payments fetched", service.getPaymentsBySale(saleId), HttpStatus.OK)
		);
	}

	@GetMapping("/sale/{saleId}/summary")
	public ResponseEntity<ApiResponse<Map<String, Object>>> getSalePaymentSummary(@PathVariable UUID saleId) {
		Map<String, Object> map = new HashMap<>();
		map.put("totalSaleAmount", service.getTotalSaleAmount(saleId));
		map.put("totalReceived", service.getTotalReceived(saleId));
		map.put("outstandingAmount", service.getOutstandingAmount(saleId));
		map.put("commissionPaid", service.getCommissionPaid(saleId));	

		return ResponseEntity.ok(
			ApiResponse.success("Payment summary", map, HttpStatus.OK)
		);
	}

	@GetMapping("/sale/{saleId}")
	public ResponseEntity<ApiResponse<Map<String, Object>>> getSalePayments(@PathVariable UUID saleId) {

		Map<String, Object> map = new HashMap<>();
		map.put("totalReceived", service.getTotalReceived(saleId));
		map.put("totalPaid", service.getCommissionPaid(saleId));

		return ResponseEntity.ok(ApiResponse.success("Payment summary", map, HttpStatus.OK));
	}
	
	@PatchMapping("/{paymentId}/verify")
	public ResponseEntity<ApiResponse<Void>> verifyPayment(@PathVariable UUID paymentId) {
		service.verifyPayment(paymentId);
		return ResponseEntity.ok(
			ApiResponse.success("Payment verified", null, HttpStatus.OK)
		);
	}
	
	@DeleteMapping("/{paymentId}")
	public ResponseEntity<ApiResponse<Void>> deletePayment(@PathVariable UUID paymentId) {
		service.deletePayment(paymentId);
		return ResponseEntity.ok(
			ApiResponse.success("Payment deleted", null, HttpStatus.OK)
		);
	}


}
