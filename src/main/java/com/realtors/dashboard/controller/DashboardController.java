package com.realtors.dashboard.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.realtors.dashboard.dto.DashboardResponseDTO;
import com.realtors.dashboard.dto.InventoryStatusDTO;
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
    public DashboardResponseDTO getDashboard(
    		@RequestParam(required = false) LocalDate from,
    	    @RequestParam(required = false) LocalDate to,
    		@AuthenticationPrincipal UserPrincipalDto principal,
            @RequestParam(required = false) UUID projectId) {
    	
    	DashboardScope scope = scopeServcie.resolve(principal);
    	 if(principal.hasRole(UserRole.CUSTOMER)) {
         	scope.setCustomerId(principal.getUserId());
         }
        scope.setFromDate(from);
        scope.setToDate(to);
        return dashboardService.getDashboard(scope);
    }
    
    @GetMapping("/inventory/details")
    public List<InventoryStatusDTO> getInventoryDetails(
        @AuthenticationPrincipal UserPrincipalDto principal,
        @RequestParam(required = false) LocalDate from,
        @RequestParam(required = false) LocalDate to
    ) {
        DashboardScope scope = scopeServcie.resolve(principal);
        scope.setFromDate(from);
        scope.setToDate(to);
        return dashboardService.getInventoryDetails(scope);
    }
}
