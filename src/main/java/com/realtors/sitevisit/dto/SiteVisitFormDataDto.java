package com.realtors.sitevisit.dto;

import java.util.List;

import com.realtors.customers.dto.CustomerMiniDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class SiteVisitFormDataDto {
    private List<AgentDto> agents;
    private List<ProjectMiniDto> projects;
    private List<CustomerMiniDto> customers;
}
