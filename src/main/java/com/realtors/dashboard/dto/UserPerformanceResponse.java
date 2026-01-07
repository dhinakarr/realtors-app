package com.realtors.dashboard.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class UserPerformanceResponse {
	private UserPerformanceKpiDTO kpi;
	private PagedResponse<UserPerformanceSnapshotDTO> snapshot;
	
}
