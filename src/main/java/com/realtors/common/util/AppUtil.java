package com.realtors.common.util;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class AppUtil {
	
	public static UUID convertStringToUUID(HttpServletRequest request ) {
    	String userIdStr = (String) request.getAttribute("userId");
    	return UUID.fromString(userIdStr);
    }

	public static UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) return null;
        String userIdStr = auth.getPrincipal().toString();
        try {
            return UUID.fromString(userIdStr);
        } catch (Exception e) {
            return null;
        }
    }
	
	public static BigDecimal nz(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value;
	}
}
