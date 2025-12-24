package com.realtors.dashboard.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class InventorySummaryDTO {
    private UUID projectId;
    private String projectName;
    private int totalPlots;
    private int available;
    private int booked;
    private int sold;
}
