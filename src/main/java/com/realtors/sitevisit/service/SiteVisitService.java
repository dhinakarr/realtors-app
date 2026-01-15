package com.realtors.sitevisit.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.realtors.admin.dto.UserBasicDto;
import com.realtors.admin.dto.form.DynamicFormMetaRow;
import com.realtors.admin.dto.form.DynamicFormResponseDto;
import com.realtors.admin.dto.form.EditResponseDto;
import com.realtors.admin.service.AbstractBaseService;
import com.realtors.admin.service.UserService;
import com.realtors.common.util.AppUtil;
import com.realtors.customers.dto.CustomerMiniDto;
import com.realtors.customers.service.CustomerService;
import com.realtors.sitevisit.dto.SiteVisitResponseDTO;
import com.realtors.sitevisit.repository.SiteVisitCustomerRepository;

@Service
public class SiteVisitService extends AbstractBaseService<SiteVisitResponseDTO, UUID> {
	@Autowired
	private final JdbcTemplate jdbcTemplate;
	private final SiteVisitCustomerRepository repo;
	private final CustomerService customerService;
	private final UserService userService;
	private static final Logger logger = LoggerFactory.getLogger(SiteVisitService.class);

	public SiteVisitService(JdbcTemplate jdbcTemplate, SiteVisitCustomerRepository repo, 
										UserService userService, CustomerService customerService) {
		super(SiteVisitResponseDTO.class, "site_visits", jdbcTemplate);
		this.repo = repo;
		this.jdbcTemplate = jdbcTemplate;
		this.userService = userService;
		this.customerService = customerService;
	}

	@Override
	protected String getIdColumn() {
		return "site_visit_id";
	}

	public DynamicFormResponseDto getVisitFormData(boolean isCommonRole) {
		DynamicFormResponseDto form = super.buildDynamicFormResponse();
		if (!isCommonRole) {
	        filterUserLookup(form); // self + subordinates only
	    }
		return form;
	}

	public EditResponseDto<SiteVisitResponseDTO> editEditFormResponse(UUID visitId) {
		Optional<SiteVisitResponseDTO> opt = super.findById(visitId);
		DynamicFormResponseDto form = super.buildDynamicFormResponse();
		filterUserLookup(form);
		return opt.map(siteVisit -> {
			// fetch customers for this visit
			List<CustomerMiniDto> customers = repo
					.findCustomers(visitId).stream().map(c -> new CustomerMiniDto(c.getCustomerId(),
							c.getCustomerName(), c.getMobile(), c.getEmail(), c.getSoldBy()))
					.collect(Collectors.toList());

			// attach to the existing siteVisit DTO
			siteVisit.setCustomers(customers);
			return new EditResponseDto<>(siteVisit, form);
		}).orElse(null);
	}

	private void filterUserLookup(DynamicFormResponseDto form) {
		UUID currentUserId = AppUtil.getCurrentUserId();
	    List<UserBasicDto> subordinates = userService.findSubordinates();
	    List<CustomerMiniDto> customers = customerService.getCustomersVisibleToUser(currentUserId);
	    Set<UUID> allowedCustomerIds = customers.stream().map(CustomerMiniDto::getCustomerId).collect(Collectors.toSet());
	    
	    Set<UUID> allowedUserIds = subordinates.stream()
	            .map(UserBasicDto::userId)
	            .collect(Collectors.toSet());
	    
	    for (DynamicFormMetaRow row : form.getFields()) {
	        if ("user_id".equals(row.getColumnName())
	                && "select".equalsIgnoreCase(row.getFieldType())
	                && row.getLookupData() != null) {

	            List<Map<String, Object>> lookupData = (List<Map<String, Object>>) row.getLookupData();
	            List<Map<String, Object>> filtered = lookupData.stream()
	                    .filter(m -> {
	                        Object keyObj = m.get("key");
	                        return keyObj instanceof UUID
	                                && allowedUserIds.contains((UUID) keyObj);
	                    })
	                    .collect(Collectors.toList());
	            row.setLookupData(filtered);
	        }
	        
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
