package com.realtors.dashboard.dto;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class AgentPerformanceDTO {
    private UUID agentId;
    private String agentName;
    private long totalSales;
    private BigDecimal salesValue;
}
