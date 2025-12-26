package com.realtors.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class InventoryDashboardDTO {
    private List<InventoryDetailDTO> overallStats;
    private List<ProjectInventoryStatsDTO> projectStats;
}

