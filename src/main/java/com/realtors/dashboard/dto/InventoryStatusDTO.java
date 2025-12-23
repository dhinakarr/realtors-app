package com.realtors.dashboard.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
@AllArgsConstructor
public class InventoryStatusDTO {

    private UUID plotId;
    private UUID projectId;
    private String projectName;
    private String plotNumber;
    private String inventoryStatus;

    // getters & setters
}
