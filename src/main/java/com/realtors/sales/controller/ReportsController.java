package com.realtors.sales.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.realtors.sales.dto.SaleCommissionDTO;
import com.realtors.sales.dto.SaleDTO;
import com.realtors.sales.service.CommissionService;
import com.realtors.sales.service.SaleService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportsController {

    private final SaleService saleService;
    private final CommissionService commissionService;

    @GetMapping("/sales")
    public ResponseEntity<List<SaleDTO>> salesReport(@RequestParam String fromDate,
                                                     @RequestParam String toDate) {
//        return ResponseEntity.ok(saleService.getSalesReport(fromDate, toDate));
    	return null;
    }

    @GetMapping("/commissions")
    public ResponseEntity<List<SaleCommissionDTO>> commissionReport(@RequestParam UUID userId) {
//        return ResponseEntity.ok(commissionService.getCommissions(null, userId));
    	return null;
    }
}

