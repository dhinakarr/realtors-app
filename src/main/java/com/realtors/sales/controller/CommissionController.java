package com.realtors.sales.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.realtors.common.ApiResponse;
import com.realtors.sales.dto.SaleCommissionDTO;
import com.realtors.sales.service.CommissionService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/commissions")
@RequiredArgsConstructor
public class CommissionController {

    private final CommissionService commissionService;

    @PostMapping
    public ResponseEntity<ApiResponse<SaleCommissionDTO>> createCommission(@RequestBody SaleCommissionDTO dto) {
    	SaleCommissionDTO data = commissionService.insertCommission(dto);
        return ResponseEntity.ok(ApiResponse.success("Sales commission data created", data, HttpStatus.CREATED));
    }

    @PatchMapping("/{commissionId}")
    public ResponseEntity<ApiResponse<SaleCommissionDTO>> updateCommission(
            @PathVariable UUID commissionId,
            @RequestBody SaleCommissionDTO dto) {
        return ResponseEntity.ok(ApiResponse.success("Sales commission data updated",commissionService.updateCommission(commissionId, dto)));
    }

    @GetMapping
    public ResponseEntity<List<SaleCommissionDTO>> getCommissions(
            @RequestParam(required = false) UUID saleId,
            @RequestParam(required = false) UUID userId) {
        return ResponseEntity.ok(commissionService.getCommissionsBySale(saleId));
    }
}
