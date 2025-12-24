package com.realtors.dashboard.service;

import java.util.UUID;

import org.springframework.stereotype.Service;

@Service
public class DashboardOldService {

    private final ReceivableService receivableService;
//    private final CommissionService commissionService;
    private final InventoryService inventoryService;

    public DashboardOldService(
            ReceivableService receivableService,
//            CommissionService commissionService,
            InventoryService inventoryService) {

        this.receivableService = receivableService;
//        this.commissionService = commissionService;
        this.inventoryService = inventoryService;
    }

	/*
	 * public CustomerDashboardDTO buildCustomer(UUID userId) { return
	 * receivableService.buildCustomerDashboard(userId); }
	 * 
	 * public AgentDashboardDTO buildAgent(UUID agentId) { return new
	 * AgentDashboardDTO( receivableService.buildAgentReceivables(agentId),
	 * commissionService.buildAgentCommissions(agentId) ); }
	 * 
	 * public FinanceDashboardDTO buildFinance() { return new FinanceDashboardDTO(
	 * receivableService.getAllReceivables(), commissionService.getAllCommissions()
	 * ); }
	 * 
	 * public InventoryDashboardDTO buildInventory() { return
	 * inventoryService.getInventoryDashboard(); }
	 */
}
