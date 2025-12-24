package com.realtors.dashboard.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.realtors.admin.dto.RoleDto;
import com.realtors.admin.service.RoleService;
import com.realtors.common.util.AppUtil;
import com.realtors.common.util.JwtUtil;
import com.realtors.dashboard.dto.DashboardContext;
import com.realtors.dashboard.dto.DashboardResponse;
import com.realtors.dashboard.dto.UserRole;
import com.realtors.dashboard.service.DashboardFacade;
import com.realtors.dashboard.service.strategy.DashboardStrategy;
import com.realtors.dashboard.service.strategy.DashboardStrategyFactory;
import com.realtors.dashboard.service.strategy.DashboardStrategyResolver;

import io.jsonwebtoken.Claims;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardOldController {

	private final RoleService roleService;
	private final DashboardFacade dashboardFacade;
	private final DashboardStrategyFactory factory; 
	private final JwtUtil jwtUtil;

	public DashboardOldController(JwtUtil jwtUtil, RoleService roleService, DashboardFacade dashboardFacade,
														DashboardStrategyFactory factory) {
		this.jwtUtil = jwtUtil;
		this.roleService = roleService;
		this.dashboardFacade = dashboardFacade;
		this.factory = factory;
	}

	@GetMapping
	public DashboardResponse getDashboard(@RequestHeader("Authorization") String header) {
		
		String token = header.substring(7);
		Claims claims = jwtUtil.extractClaims(token);
		UUID userId = AppUtil.getCurrentUserId();
		String roleId = claims.get("roleId", String.class);
		UUID role = UUID.fromString(roleId);
		
		/*
		 * RoleDto roleDto = roleService.findById(role).orElse(null); String roleCode =
		 * roleDto.getFinanceRole();
		 * 
		 * UserRole userRole = UserRole.from(roleCode);
		 * 
		 * DashboardStrategy strategy = factory.getStrategy(userRole);
		 * 
		 * if (strategy == null) { throw new IllegalStateException(
		 * "No dashboard strategy for role " + role ); }
		 * 
		 * return strategy.buildDashboard( new DashboardContext(userId, userRole) );
		 */
		return null;
	}
}
