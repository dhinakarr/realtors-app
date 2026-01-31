package com.realtors.common.validator;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.realtors.admin.service.AclPermissionService;
import com.realtors.common.util.AppUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class PermissionInterceptor implements HandlerInterceptor {

	private final PermissionRegistry registry;
	private final AclPermissionService permissionService;
	private static final Logger logger = LoggerFactory.getLogger(PermissionInterceptor.class);

	public PermissionInterceptor(PermissionRegistry registry, AclPermissionService permissionService) {
		this.registry = registry;
		this.permissionService = permissionService;
	}

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {

		String path = request.getRequestURI();
		String method = request.getMethod();
		String feature = registry.resolveFeature(path);
		logger.info("@PermissionInterceptor.preHandle path={}, method={}, feature={}", path, method, feature);
		
		if (feature == null) {
			return true; // public or unmanaged API
		}

		PermissionAction action = ActionResolver.fromHttpMethod(method);
		UUID roleId = AppUtil.getCurrentRoleId();
		boolean allowed = permissionService.hasPermission(roleId, feature, action);
		
		logger.info("@PermissionInterceptor.preHandle feature={}, roleId={}, alllowed={}", feature, roleId, allowed);
		
		if (!allowed) {
			response.sendError(HttpStatus.FORBIDDEN.value(), "Permission denied");
			return false;
		}

		return true;
	}
}
