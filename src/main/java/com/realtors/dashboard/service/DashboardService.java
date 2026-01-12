package com.realtors.dashboard.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.realtors.dashboard.dto.AgentPerformanceDTO;
import com.realtors.dashboard.dto.CommissionSummaryDTO;
import com.realtors.dashboard.dto.DashboardKPI;
import com.realtors.dashboard.dto.DashboardKpiDTO;
import com.realtors.dashboard.dto.DashboardKpiWrapperDTO;
import com.realtors.dashboard.dto.DashboardPermission;
import com.realtors.dashboard.dto.DashboardResponseDTO;
import com.realtors.dashboard.dto.DashboardScope;
import com.realtors.dashboard.dto.FinancialSummaryDTO;
import com.realtors.dashboard.dto.InventoryStatusDTO;
import com.realtors.dashboard.dto.InventorySummaryDTO;
import com.realtors.dashboard.dto.SiteVisitSummaryDTO;
import com.realtors.dashboard.repository.DashboardAgentRepository;
import com.realtors.dashboard.repository.DashboardCommissionRepository;
import com.realtors.dashboard.repository.DashboardFinanceRepository;
import com.realtors.dashboard.repository.DashboardInventoryRepository;
import com.realtors.dashboard.repository.DashboardRepository;
import com.realtors.dashboard.repository.DashboardSiteVisitRepository;

@Service
public class DashboardService {

	private DashboardInventoryRepository inventoryRepo;
	private DashboardFinanceRepository financeRepo;
	private DashboardAgentRepository agentRepo;
	private DashboardCommissionRepository commissionRepo;
	private DashboardSiteVisitRepository siteVisitRepo;
	private DashboardRepository dashRepo;
	private static final Logger logger = LoggerFactory.getLogger(DashboardService.class);

	public DashboardService(DashboardInventoryRepository inventoryRepo, DashboardFinanceRepository financeRepo,
			DashboardAgentRepository agentRepo, DashboardCommissionRepository commissionRepo,
			DashboardSiteVisitRepository siteVisitRepo, DashboardScopeService scopeServcie,
			DashboardRepository dashRepo) {

		this.inventoryRepo = inventoryRepo;
		this.financeRepo = financeRepo;
		this.agentRepo = agentRepo;
		this.commissionRepo = commissionRepo;
		this.siteVisitRepo = siteVisitRepo;
		this.dashRepo = dashRepo;
	}

	public DashboardResponseDTO getDashboard(DashboardScope scope) {
		UUID userId = scope.getUserId();
		// Fetch summaries

		List<InventorySummaryDTO> inventory = scope.can(DashboardPermission.VIEW_INVENTORY)
				? inventoryRepo.fetchInventorySummary(scope)
				: List.of();

		List<FinancialSummaryDTO> finance = scope.can(DashboardPermission.VIEW_FINANCE)
				? financeRepo.fetchFinancialSummary(scope)
				: List.of();

		List<AgentPerformanceDTO> agents = scope.can(DashboardPermission.VIEW_AGENTS)
				? agentRepo.fetchAgentPerformance(scope)
				: List.of();

		List<CommissionSummaryDTO> commissions = scope.can(DashboardPermission.VIEW_COMMISSIONS)
				? commissionRepo.fetchCommissionSummary(scope)
				: List.of();

		List<SiteVisitSummaryDTO> siteVisits = scope.can(DashboardPermission.VIEW_SITE_VISITS)
				? siteVisitRepo.fetchSiteVisitSummary(scope)
				: List.of();

		// =================== Reuse buildKpis ===================
		DashboardKpiDTO summaryKpis = buildKpis(inventory, finance, commissions, siteVisits);
		DashboardKPI allTimeKpi = getKpis(userId);

		DashboardKpiWrapperDTO actionKpis = DashboardKpiWrapperDTO.builder().today(allTimeKpi).month(allTimeKpi)
				.total(allTimeKpi).build();
		// =================== Build response ===================
		return DashboardResponseDTO.builder().inventory(inventory).finance(finance).agents(agents)
				.commissions(commissions).siteVisits(siteVisits).summaryKpis(summaryKpis) // existing KPI summary
				.actionKpis(actionKpis) // new Today/Month/Total KPIs
				.build();
	}

	public List<InventoryStatusDTO> getInventoryDetails(DashboardScope scope) {
		return inventoryRepo.fetchInventoryDetails(scope);
	}

	public DashboardKPI getKpis(UUID userId) {
		DashboardKPI kpi = new DashboardKPI();
		kpi.setLeadsCreated(dashRepo.countLeadsCreated(userId));
		kpi.setFollowUpsDue(dashRepo.countFollowUpsToday(userId));
		kpi.setSiteVisitsScheduled(dashRepo.countSiteVisitsScheduled(userId));
		kpi.setBookingsDone(dashRepo.countBookingsDone(userId));
		kpi.setCommissionApproved(dashRepo.sumCommissionEarned(userId));
		kpi.setCommissionPending(dashRepo.sumCommissionPending(userId));
		return kpi;
	}

	private DashboardKpiDTO buildKpis(List<InventorySummaryDTO> inventory, List<FinancialSummaryDTO> finance,
			List<CommissionSummaryDTO> commissions, List<SiteVisitSummaryDTO> visits) {

		return DashboardKpiDTO.builder()
				.totalPlots(inventory.stream().mapToInt(InventorySummaryDTO::getTotalPlots).sum())
				.availablePlots(inventory.stream().mapToInt(InventorySummaryDTO::getAvailable).sum())
				.bookedPlots(inventory.stream().mapToInt(InventorySummaryDTO::getBooked).sum())
				.totalSales(finance.stream().map(FinancialSummaryDTO::getTotalSales).filter(Objects::nonNull)
						.reduce(BigDecimal.ZERO, BigDecimal::add))
				.totalReceived(finance.stream().map(FinancialSummaryDTO::getTotalReceived).filter(Objects::nonNull)
						.reduce(BigDecimal.ZERO, BigDecimal::add))
				.totalOutstanding(finance.stream().map(FinancialSummaryDTO::getTotalOutstanding)
						.filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add))
				.totalCommissionPayable(commissions.stream().map(CommissionSummaryDTO::getTotalPayable)
						.filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add))
				.totalSiteVisits(visits.stream().mapToLong(SiteVisitSummaryDTO::getTotalVisits).sum())
				.avgConversionRatio(visits.isEmpty() ? BigDecimal.ZERO
						: visits.stream().map(SiteVisitSummaryDTO::getConversionRatio).filter(Objects::nonNull)
								.reduce(BigDecimal.ZERO, BigDecimal::add)
								.divide(BigDecimal.valueOf(visits.size()), 2, RoundingMode.HALF_UP))

				.build();
	}

}
