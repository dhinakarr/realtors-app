package com.realtors.common.validator;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class PermissionRegistry {
	private static final Map<String, String> URL_FEATURE_MAP;

	static {
		Map<String, String> map = new LinkedHashMap<>();

		map.put("/api/commission-rules", "COMMISSION_RULES");
		map.put("/api/commissions", "COMMISSIONS");
		map.put("/api/site-visits", "SITE_VISITS");
		map.put("/api/projects", "PROJECTS");
		map.put("/api/customers", "CUSTOMERS");
		map.put("/api/finance", "FINANCE");
		map.put("/api/dashboard", "DASHBOARD");
		map.put("/api/permissions", "PERMISSIONS");
		map.put("/api/roles", "ROLES");
		map.put("/api/users", "USERS");
		map.put("/api/modules", "MODULES");
		map.put("/api/features", "FEATURES");
		map.put("/api/sales", "SALES");
		map.put("/api/plots", "PLOTS");
		map.put("/api/payments", "PAYMENTS");

		URL_FEATURE_MAP = Collections.unmodifiableMap(map);
	}

	public String resolveFeature(String path) {
		return URL_FEATURE_MAP.entrySet().stream().filter(e -> path.startsWith(e.getKey())).map(Map.Entry::getValue)
				.findFirst().orElse(null);
	}
}
