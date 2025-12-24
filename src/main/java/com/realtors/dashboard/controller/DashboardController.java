package com.realtors.dashboard.controller;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.realtors.dashboard.dto.DashboardResponseDTO;
import com.realtors.dashboard.dto.UserPrincipalDto;
import com.realtors.dashboard.service.DashboardService;


@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);
    
    public DashboardController(DashboardService dashboardService) {
    	this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public DashboardResponseDTO getDashboard(@AuthenticationPrincipal UserPrincipalDto principal,
            @RequestParam(required = false) UUID projectId) {
    	logger.info("@DashboardController.getDashboard principal: "+principal.toString());
        return dashboardService.getDashboard(principal);
    }
}
