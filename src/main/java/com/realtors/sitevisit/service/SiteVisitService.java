package com.realtors.sitevisit.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.realtors.admin.dto.form.DynamicFormResponseDto;
import com.realtors.admin.dto.form.EditResponseDto;
import com.realtors.admin.service.AbstractBaseService;
import com.realtors.customers.dto.CustomerMiniDto;
import com.realtors.sitevisit.dto.SiteVisitResponseDTO;
import com.realtors.sitevisit.repository.SiteVisitCustomerRepository;

import lombok.RequiredArgsConstructor;


@Service
public class SiteVisitService extends AbstractBaseService<SiteVisitResponseDTO, UUID>{
	@Autowired
	private final JdbcTemplate jdbcTemplate;
	private final SiteVisitCustomerRepository repo;

	public SiteVisitService(JdbcTemplate jdbcTemplate, SiteVisitCustomerRepository repo) {
		super(SiteVisitResponseDTO.class, "site_visits", jdbcTemplate);
		this.repo = repo;
		this.jdbcTemplate = jdbcTemplate;
	}

	@Override
	protected String getIdColumn() {
		return "site_visit_id";
	}
	
	public DynamicFormResponseDto getVisitFormData() {
		return super.buildDynamicFormResponse();
	}
	
	public EditResponseDto<SiteVisitResponseDTO> editEditFormResponse(UUID visitId) {
		Optional<SiteVisitResponseDTO> opt = super.findById(visitId);
		DynamicFormResponseDto form = super.buildDynamicFormResponse();
		
		return opt.map(siteVisit -> {
	        // fetch customers for this visit
	        List<CustomerMiniDto> customers = repo.findCustomers(visitId)
	                .stream()
	                .map(c -> new CustomerMiniDto(
	                        c.getCustomerId(),
	                        c.getCustomerName(),
	                        c.getMobile(),
	                        c.getSoldBy()
	                ))
	                .collect(Collectors.toList());

	        // attach to the existing siteVisit DTO
	        siteVisit.setCustomers(customers);
	        return new EditResponseDto<>(siteVisit, form);
	    }).orElse(null);
	}
}
