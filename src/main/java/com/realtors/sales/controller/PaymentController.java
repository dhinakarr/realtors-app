package com.realtors.sales.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.realtors.common.ApiResponse;
import com.realtors.sales.dto.PaymentDTO;
import com.realtors.sales.service.PaymentService;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

	@Autowired
	private PaymentService service;

	@PostMapping
	public ResponseEntity<ApiResponse<PaymentDTO>> createPayment(@RequestBody PaymentDTO dto) {
		PaymentDTO saved = service.save(dto);
		return ResponseEntity.ok(ApiResponse.success("Payment saved", saved, HttpStatus.OK));
	}

	@GetMapping("/sale/{saleId}")
	public ResponseEntity<ApiResponse<Map<String, Object>>> getSalePayments(@PathVariable UUID saleId) {

		Map<String, Object> map = new HashMap<>();
		map.put("totalReceived", service.getOutstanding(saleId));
		map.put("totalPaid", service.getCommissionPaid(saleId));

		return ResponseEntity.ok(ApiResponse.success("Payment summary", map, HttpStatus.OK));
	}
}
