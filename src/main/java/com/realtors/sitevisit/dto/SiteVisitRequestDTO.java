package com.realtors.sitevisit.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
public class SiteVisitRequestDTO {

    private LocalDate visitDate;
    private UUID userId;          // agent
    private UUID projectId;
    private List<UUID> customerId;
    private String vehicleType;
    private BigDecimal expenseAmount;
    private String remarks;
}
