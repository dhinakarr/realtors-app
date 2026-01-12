package com.realtors.dashboard.service;

import java.util.HashSet;
import java.util.Set;

import com.realtors.dashboard.dto.DashboardPermission;
import com.realtors.dashboard.dto.UserRole;

public class DashboardPermissionMapper {

	public static Set<DashboardPermission> forRoles(Set<UserRole> roles) {
		Set<DashboardPermission> perms = new HashSet<>();

		if (roles.contains(UserRole.MD)) {
			perms.addAll(Set.of(DashboardPermission.values()));
		}

		if (roles.contains(UserRole.FINANCE)) {
			perms.add(DashboardPermission.VIEW_FINANCE);
			perms.add(DashboardPermission.VIEW_COMMISSIONS);
			perms.add(DashboardPermission.VIEW_KPIS);
		}

		if (roles.contains(UserRole.HR)) {
			perms.add(DashboardPermission.VIEW_INVENTORY);
			perms.add(DashboardPermission.VIEW_FINANCE);
			perms.add(DashboardPermission.VIEW_SITE_VISITS);
		}

		if (roles.contains(UserRole.PA) || roles.contains(UserRole.PM) || roles.contains(UserRole.PH)) {
			perms.add(DashboardPermission.VIEW_INVENTORY);
			perms.add(DashboardPermission.VIEW_SITE_VISITS);
			perms.add(DashboardPermission.VIEW_FINANCE);
			perms.add(DashboardPermission.VIEW_COMMISSIONS);
			perms.add(DashboardPermission.VIEW_AGENTS);
			perms.add(DashboardPermission.VIEW_KPIS);
		}

		if (roles.contains(UserRole.CUSTOMER)) {
			perms.add(DashboardPermission.VIEW_SITE_VISITS);
			perms.add(DashboardPermission.VIEW_FINANCE);
		    perms.add(DashboardPermission.VIEW_INVENTORY);
		}

		return perms;
	}
}
