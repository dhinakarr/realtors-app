package com.realtors.dashboard.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.realtors.common.ApiResponse;
import com.realtors.dashboard.dto.CommissionDetailsDTO;
import com.realtors.dashboard.dto.DashboardResponseDTO;
import com.realtors.dashboard.dto.InventoryStatusDTO;
import com.realtors.dashboard.dto.ReceivableDetailDTO;
import com.realtors.dashboard.dto.SiteVisitDetailsDTO;
import com.realtors.dashboard.dto.DashboardScope;
import com.realtors.dashboard.dto.UserPrincipalDto;
import com.realtors.dashboard.dto.UserRole;
import com.realtors.dashboard.service.DashboardScopeService;
import com.realtors.dashboard.service.DashboardService;


@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private DashboardScopeService scopeServcie;
    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);
    
    public DashboardController(DashboardService dashboardService, DashboardScopeService scopeServcie) {
    	this.dashboardService = dashboardService;
    	this.scopeServcie = scopeServcie;
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<DashboardResponseDTO>> getDashboard(
    		@RequestParam(required = false) LocalDate from,
    	    @RequestParam(required = false) LocalDate to,
    		@AuthenticationPrincipal UserPrincipalDto principal,
            @RequestParam(required = false) UUID projectId) {
    	
    	DashboardScope scope = scopeServcie.resolve(principal);
    	 if(principal.hasRole(UserRole.CUSTOMER)) {
         	scope.setCustomerId(principal.getUserId());
         } else if (principal.hasRole(UserRole.MD) || principal.hasRole(UserRole.FINANCE)) {
        	 scope.setTeamView(true);
         }
    	 
        scope.setFromDate(from);
        scope.setToDate(to);
        return ResponseEntity.ok(ApiResponse.success("Dashboard Summary fetched", dashboardService.getDashboard(scope), HttpStatus.OK));
    }
    
    @GetMapping("/inventory/details")
    public ResponseEntity<ApiResponse<List<InventoryStatusDTO>>> getInventoryDetails(
        @AuthenticationPrincipal UserPrincipalDto principal,
        @RequestParam(required = false) LocalDate from,
        @RequestParam(required = false) LocalDate to
    ) {
        DashboardScope scope = scopeServcie.resolve(principal);
        scope.setFromDate(from);
        scope.setToDate(to);
        return ResponseEntity.ok(ApiResponse.success("Dashboard Summary fetched", dashboardService.getInventoryDetails(scope), HttpStatus.OK));
    }
    
    @GetMapping("/sales/details")
    public ResponseEntity<ApiResponse<List<ReceivableDetailDTO>>> getReceivableDetails(@AuthenticationPrincipal UserPrincipalDto principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    	
    	DashboardScope scope = scopeServcie.resolve(principal);
        scope.setFromDate(from);
        scope.setToDate(to);
        List<ReceivableDetailDTO> data = dashboardService.getReceivableDetails(scope);
    	return ResponseEntity.ok(ApiResponse.success("Dashboard Summary fetched", data, HttpStatus.OK));
    }
    
    @GetMapping("/commission/details")
    public ResponseEntity<ApiResponse<List<CommissionDetailsDTO>>> getComissionDetails(@AuthenticationPrincipal UserPrincipalDto principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    	
    	DashboardScope scope = scopeServcie.resolve(principal);
        scope.setFromDate(from);
        scope.setToDate(to);
        List<CommissionDetailsDTO> data = dashboardService.getCommissionDetails(scope);
    	return ResponseEntity.ok(ApiResponse.success("Dashboard Summary fetched", data, HttpStatus.OK));
    }
    
    @GetMapping("/visit/details")
    public ResponseEntity<ApiResponse<List<SiteVisitDetailsDTO>>> getSiteVisitDetails(@AuthenticationPrincipal UserPrincipalDto principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    	
    	DashboardScope scope = scopeServcie.resolve(principal);
        scope.setFromDate(from);
        scope.setToDate(to);
        List<SiteVisitDetailsDTO> data = dashboardService.fetchSiteVisitDetails(scope);
        return ResponseEntity.ok(ApiResponse.success("Dashboard Summary fetched", data, HttpStatus.OK));
    }
}
