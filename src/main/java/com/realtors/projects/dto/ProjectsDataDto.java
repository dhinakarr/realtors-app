package com.realtors.projects.dto;

import java.util.List;

import com.realtors.dashboard.dto.InventorySummaryDTO;

public record ProjectsDataDto(List<ProjectSummaryDto> summary, List<InventorySummaryDTO> inventory) {


}
