package com.realtors.dashboard.service;

import org.springframework.stereotype.Service;

import com.realtors.dashboard.dto.InventoryDashboardDTO;
import com.realtors.dashboard.repository.InventoryRepository;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    public InventoryService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    public InventoryDashboardDTO getInventoryDashboard() {

        InventoryDashboardDTO dto = new InventoryDashboardDTO();

        dto.setOverallStats(
            inventoryRepository.getOverallInventory()
        );

        dto.setProjectStats(
            inventoryRepository.getProjectWiseInventory()
        );

        return dto;
    }
}
