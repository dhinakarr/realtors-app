package com.realtors.dashboard.service;

import java.util.UUID;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.realtors.dashboard.dto.UserRole;
import com.realtors.dashboard.repository.DashboardScopeRepository;
import com.realtors.admin.service.UserHierarchyService;
import com.realtors.dashboard.dto.DashboardPermission;
import com.realtors.dashboard.dto.DashboardScope;
import com.realtors.dashboard.dto.UserPrincipalDto;

@Service
public class DashboardScopeService {

	private final DashboardScopeRepository scopeRepo;
	private UserHierarchyService hierarchyService;
	
	private static final Logger logger = LoggerFactory.getLogger(DashboardScopeService.class);
	
	public DashboardScopeService(DashboardScopeRepository scopeRepo, UserHierarchyService hierarchyService) {
		this.scopeRepo  = scopeRepo;
		this.hierarchyService = hierarchyService;
	}

	public DashboardScope resolve(UserPrincipalDto user) {
		Set<UserRole> roles = user.getRoles();
		UUID userId = user.getUserId();
		
		Set<DashboardPermission> permissions = DashboardPermissionMapper.forRoles(roles);
		
		// MD → everything
		if (roles.contains(UserRole.MD)) {
			return DashboardScope.builder()
					.all(true)
					.userId(userId)
					.userIds(Set.of(userId))
					.permissions(permissions)
					.build();
		}

		// FINANCE / HR → all projects, but limited usage
		if (roles.contains(UserRole.FINANCE) || roles.contains(UserRole.HR)) {
			return DashboardScope.builder()
					.all(true)
					.userId(userId)
					.financeOnly(roles.contains(UserRole.FINANCE))
					.hrOnly(roles.contains(UserRole.HR))
					.permissions(permissions)
					.build();
		}

		// PA → user-level scope
		if (roles.contains(UserRole.PA)) {
			return DashboardScope.builder()
					.userId(userId)
					.userIds(Set.of(userId))
					.permissions(permissions)
					.build();
		}
		
		// CUSTOMER → self only (IMPORTANT)
	    if (roles.contains(UserRole.CUSTOMER)) {
	    	Set<UUID> projects = scopeRepo.findProjectsForUser(Set.of(userId));
	        return DashboardScope.builder()
	                .userId(userId)
	                .customerId(userId)  
	                .projectIds(projects)
	                .permissions(permissions)
	                .build();
	    }
		
		// PM / PH → project-level scope
		if (roles.contains(UserRole.PM) || roles.contains(UserRole.PH)) {
			Set<UUID> subordinates = new HashSet<>(hierarchyService.getAllSubordinates(userId));

	        // include self
	        subordinates.add(userId);
			Set<UUID> projects = scopeRepo.findProjectsForUser(subordinates);
			return DashboardScope.builder()
					.userId(userId)
					.userIds(subordinates)
					.projectIds(projects)
					.permissions(permissions)
					.build();
		}
		throw new IllegalStateException("Unsupported role: " + roles);
	}

}
