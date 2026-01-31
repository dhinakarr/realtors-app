package com.realtors.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.realtors.admin.dto.UserBasicDto;
import com.realtors.admin.dto.form.DynamicFormMetaRow;
import com.realtors.admin.dto.form.DynamicFormResponseDto;
import com.realtors.customers.dto.CustomerMiniDto;
import com.realtors.dashboard.dto.UserPrincipalDto;
import com.realtors.dashboard.dto.UserRole;

public class AppUtil {
	
	private static final Logger logger = LoggerFactory.getLogger(AppUtil.class);
	
	public static UUID convertStringToUUID(HttpServletRequest request ) {
    	String userIdStr = (String) request.getAttribute("userId");
    	return UUID.fromString(userIdStr);
    }
	
	public static String getFormattedDate(LocalDate date) {
		return date.format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
	}

	public static UUID getCurrentUserIdOld() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) return null;
        String userIdStr = auth.getPrincipal().toString();
       System.out.println("@AppUtil.getCurrentUserId: "+userIdStr);
        try {
            return UUID.fromString(userIdStr);
        } catch (Exception e) {
            return null;
        }
    }
	
	public static UUID getCurrentUserId() {
	    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
	    if (auth == null || !(auth.getPrincipal() instanceof UserPrincipalDto p)) {
	        return null;
	    }
	    return p.getUserId();
	}
	
	public static UUID getCurrentRoleId() {
	    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
	    if (auth == null || !(auth.getPrincipal() instanceof UserPrincipalDto p)) {
	        return null;
	    }
	    return p.getRoleId();
	}
	
	
	public static BigDecimal nz(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value;
	}
	
	public static BigDecimal percent(BigDecimal value) {
	    return nz(value)
	        .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
	}
	
	public static boolean isCommonRole(UserPrincipalDto principal) {
    	Set<UserRole> role = principal.getRoles();
    	Set<UserRole> commonRole = Set.of(UserRole.FINANCE, UserRole.MD, UserRole.HR);
    	return role.stream().anyMatch(commonRole::contains);
    }
	
	public static void filterUserLookUp(
	        DynamicFormResponseDto form,
	        List<UserBasicDto> subordinates) {

	    if (subordinates == null || subordinates.isEmpty()) {
	        return;
	    }

	    Map<UUID, String> employeeIdByUserId =
	            subordinates.stream()
	                .collect(Collectors.toMap(
	                    UserBasicDto::userId,
	                    UserBasicDto::employeeId
	                ));

	    Set<UUID> allowedUserIds = employeeIdByUserId.keySet();

	    for (DynamicFormMetaRow row : form.getFields()) {

	        if (!"manager_id".equals(row.getColumnName())
	                || !"select".equalsIgnoreCase(row.getFieldType())
	                || row.getLookupData() == null) {
	            continue;
	        }

	        logger.info("@AppUtil.filterLookup matched manager_id row");

	        List<Map<String, Object>> lookupData =
	                (List<Map<String, Object>>) row.getLookupData();

	        List<Map<String, Object>> filtered = lookupData.stream()
	            .map(m -> {
	                Object keyObj = m.get("key");
	                if (keyObj == null) return null;

	                try {
	                    UUID userId = keyObj instanceof UUID
	                            ? (UUID) keyObj
	                            : UUID.fromString(keyObj.toString());

	                    if (!allowedUserIds.contains(userId)) {
	                        return null;
	                    }

	                    // ✅ append employeeId to display value
	                    String employeeId = employeeIdByUserId.get(userId);
	                    Object valueObj = m.get("value");

	                    if (employeeId != null && valueObj != null) {
	                        m.put("value", valueObj + " (" + employeeId + ")");
	                    }

	                    return m;

	                } catch (IllegalArgumentException e) {
	                    return null;
	                }
	            })
	            .filter(Objects::nonNull)
	            .collect(Collectors.toList());

	        row.setLookupData(filtered);
	    }
	}


	public static void filterLookup(DynamicFormResponseDto form, List<UserBasicDto> subordinates,
			List<CustomerMiniDto> customers) {
	    for (DynamicFormMetaRow row : form.getFields()) {
	    	
	    	logger.info(
	    	        "@AppUtil.filterLookup row check -> column={}, type={}, lookup={}",
	    	        row.getColumnName(),
	    	        row.getFieldType(),
	    	        row.getLookupData() != null
	    	    );
	    	
	    	
	    	if (subordinates != null && !subordinates.isEmpty()) {
	    		Set<UUID> allowedUserIds = subordinates.stream().map(UserBasicDto::userId).collect(Collectors.toSet());
		        if ("user_id".equals(row.getColumnName())
		                && "select".equalsIgnoreCase(row.getFieldType())
		                && row.getLookupData() != null) {
		        	logger.info("@AppUtil.filterLookup matched user_id row");
		            List<Map<String, Object>> lookupData = (List<Map<String, Object>>) row.getLookupData();
		            List<Map<String, Object>> filtered = lookupData.stream()
		            	    .filter(m -> {
		            	    	logger.info("@AppUtil.filterLookup subordinates: "+ m.get("key") + " -> " + m.get("key").getClass());
		            	        Object keyObj = m.get("key");
		            	        if (keyObj == null) return false;

		            	        try {
		            	            UUID keyUuid = keyObj instanceof UUID
		            	                    ? (UUID) keyObj
		            	                    : UUID.fromString(keyObj.toString());

		            	            return allowedUserIds.contains(keyUuid);
		            	        } catch (IllegalArgumentException e) {
		            	            return false;
		            	        }
		            	    })
		            	    .collect(Collectors.toList());

		            row.setLookupData(filtered);
		        }
	    	}
	        if (customers != null && !customers.isEmpty() ) {
	        	Set<UUID> allowedCustomerIds = customers.stream().map(CustomerMiniDto::getCustomerId).collect(Collectors.toSet());
		        if("customer_id".equals(row.getColumnName())
		                && "select".equalsIgnoreCase(row.getFieldType())
		                && row.getLookupData() != null) {
		        	List<Map<String, Object>> lookupData = (List<Map<String, Object>>) row.getLookupData();
		            List<Map<String, Object>> filtered = lookupData.stream()
		                    .filter(m -> {
		                        Object keyObj = m.get("key");
		                        return keyObj instanceof UUID
		                                && allowedCustomerIds.contains((UUID) keyObj);
		                    })
		                    .collect(Collectors.toList());
		            row.setLookupData(filtered);
		        }
		    }
	    }
	}
}
