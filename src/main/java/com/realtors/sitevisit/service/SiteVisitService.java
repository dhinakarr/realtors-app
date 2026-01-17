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
			getFilteredForm(form);
	    }
		return form;
	}

	public EditResponseDto<SiteVisitResponseDTO> editEditFormResponse(UUID visitId, boolean isCommonRole) {
		Optional<SiteVisitResponseDTO> opt = super.findById(visitId); 
		DynamicFormResponseDto form = super.buildDynamicFormResponse();
		if (!isCommonRole) {
	        getFilteredForm(form);
	    }
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
	
	private void getFilteredForm(DynamicFormResponseDto form) {
		UUID currentUserId = AppUtil.getCurrentUserId();
	    List<UserBasicDto> subordinates = userService.findSubordinates();
	    List<CustomerMiniDto> visibleCustomers = customerService.getCustomersVisibleToUser(currentUserId);
		AppUtil.filterLookup(form, subordinates, visibleCustomers);
	}
}
