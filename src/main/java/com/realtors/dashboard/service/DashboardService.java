package com.realtors.dashboard.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.realtors.dashboard.dto.CommissionSummaryDTO;
import com.realtors.dashboard.dto.DashboardKpiDTO;
import com.realtors.dashboard.dto.DashboardResponseDTO;
import com.realtors.dashboard.dto.DashboardScope;
import com.realtors.dashboard.dto.FinancialSummaryDTO;
import com.realtors.dashboard.dto.InventorySummaryDTO;
import com.realtors.dashboard.dto.SiteVisitSummaryDTO;
import com.realtors.dashboard.dto.UserPrincipalDto;
import com.realtors.dashboard.repository.DashboardAgentRepository;
import com.realtors.dashboard.repository.DashboardCommissionRepository;
import com.realtors.dashboard.repository.DashboardFinanceRepository;
import com.realtors.dashboard.repository.DashboardInventoryRepository;
import com.realtors.dashboard.repository.DashboardSiteVisitRepository;


@Service
public class DashboardService {

	private DashboardInventoryRepository inventoryRepo;
	private DashboardFinanceRepository financeRepo;
	private DashboardAgentRepository agentRepo;
	private DashboardCommissionRepository commissionRepo;
	private DashboardSiteVisitRepository siteVisitRepo;
	private DashboardScopeService scopeServcie;
	private static final Logger logger = LoggerFactory.getLogger(DashboardService.class);
	
	public DashboardService(
            DashboardInventoryRepository inventoryRepo,
            DashboardFinanceRepository financeRepo,
            DashboardAgentRepository agentRepo,
            DashboardCommissionRepository commissionRepo,
            DashboardSiteVisitRepository siteVisitRepo,
            DashboardScopeService scopeServcie) {

        this.inventoryRepo = inventoryRepo;
        this.financeRepo = financeRepo;
        this.agentRepo = agentRepo;
        this.commissionRepo = commissionRepo;
        this.siteVisitRepo = siteVisitRepo;
        this.scopeServcie = scopeServcie;
    }

	public DashboardResponseDTO getDashboard(UserPrincipalDto user) {

        DashboardScope scope = scopeServcie.resolve(user);
        return DashboardResponseDTO.builder()
                .inventory(scope.isHrOnly() || scope.isFinanceOnly()
                        ? List.of()
                        : inventoryRepo.fetchInventorySummary(scope))
                .finance(scope.isHrOnly()
                        ? List.of()
                        : financeRepo.fetchFinancialSummary(scope))
                .agents(agentRepo.fetchAgentPerformance(scope))
                .commissions(scope.isHrOnly()
                        ? List.of()
                        : commissionRepo.fetchCommissionSummary(scope))
                .siteVisits(siteVisitRepo.fetchSiteVisitSummary(scope))
                .build();
    }

	private DashboardKpiDTO buildKpis(List<InventorySummaryDTO> inventory, List<FinancialSummaryDTO> finance,
			List<CommissionSummaryDTO> commissions, List<SiteVisitSummaryDTO> visits) {

		return DashboardKpiDTO.builder()
				.totalPlots(inventory.stream().mapToInt(InventorySummaryDTO::getTotalPlots).sum())
				.availablePlots(inventory.stream().mapToInt(InventorySummaryDTO::getAvailable).sum())
				.bookedPlots(inventory.stream().mapToInt(InventorySummaryDTO::getBooked).sum())
				.totalSales(finance.stream().map(FinancialSummaryDTO::getTotalSales).reduce(BigDecimal.ZERO,
						BigDecimal::add))
				.totalReceived(finance.stream().map(FinancialSummaryDTO::getTotalReceived).reduce(BigDecimal.ZERO,
						BigDecimal::add))
				.totalOutstanding(finance.stream().map(FinancialSummaryDTO::getTotalOutstanding).reduce(BigDecimal.ZERO,
						BigDecimal::add))
				.totalCommissionPayable(commissions.stream().map(CommissionSummaryDTO::getTotalPayable)
						.reduce(BigDecimal.ZERO, BigDecimal::add))
				.totalSiteVisits(visits.stream().mapToLong(SiteVisitSummaryDTO::getTotalVisits).sum())
				.avgConversionRatio(visits.isEmpty() ? BigDecimal.ZERO
						: visits.stream().map(SiteVisitSummaryDTO::getConversionRatio)
								.reduce(BigDecimal.ZERO, BigDecimal::add)
								.divide(BigDecimal.valueOf(visits.size()), 2, RoundingMode.HALF_UP))
				.build();
	}

}
