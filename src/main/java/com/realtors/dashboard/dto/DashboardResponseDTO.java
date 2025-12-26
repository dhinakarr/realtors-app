package com.realtors.dashboard.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DashboardResponseDTO {

    private List<InventorySummaryDTO> inventory;
    private List<FinancialSummaryDTO> finance;
    private List<AgentPerformanceDTO> agents;
    private List<CommissionSummaryDTO> commissions;
    private List<SiteVisitSummaryDTO> siteVisits;
    private DashboardKpiWrapperDTO actionKpis;
    private DashboardKpiDTO summaryKpis;
}
