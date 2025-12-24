package com.realtors.dashboard.service;

import java.util.UUID;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.realtors.dashboard.dto.UserRole;
import com.realtors.dashboard.repository.DashboardScopeRepository;

import com.realtors.dashboard.dto.DashboardScope;
import com.realtors.dashboard.dto.UserPrincipalDto;

@Service
public class DashboardScopeService {

	private final DashboardScopeRepository scopeRepo;
	private static final Logger logger = LoggerFactory.getLogger(DashboardScopeService.class);
	
	public DashboardScopeService(DashboardScopeRepository scopeRepo) {
		this.scopeRepo  = scopeRepo;
	}

	public DashboardScope resolve(UserPrincipalDto user) {
		Set<UserRole> roles = user.getRoles();
		logger.info("@DashboardScopeService.resolve roles: "+roles.toString());
		// MD → everything
		if (roles.contains(UserRole.MD)) {
			return DashboardScope.builder().all(true).userId(user.getUserId()).build();
		}

		// FINANCE / HR → all projects, but limited usage
		if (roles.contains(UserRole.FINANCE) || roles.contains(UserRole.HR)) {
			return DashboardScope.builder().all(true).userId(user.getUserId()).financeOnly(roles.contains(UserRole.FINANCE))
					.hrOnly(roles.contains(UserRole.HR)).build();
		}

		// PA → user-level scope
		if (roles.contains(UserRole.PA)) {
			return DashboardScope.builder().userId(user.getUserId()).build();
		}

		// PM / PH → project-level scope
		if (roles.contains(UserRole.PM) || roles.contains(UserRole.PH)) {
			Set<UUID> projects = scopeRepo.findProjectsForUser(user.getUserId());
			return DashboardScope.builder().userId(user.getUserId()).projectIds(projects).build();
		}
		throw new IllegalStateException("Unsupported role: " + roles);
	}

}
