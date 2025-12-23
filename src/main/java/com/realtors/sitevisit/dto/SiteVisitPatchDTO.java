package com.realtors.sitevisit.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import lombok.Data;

@Data
public class SiteVisitPatchDTO {

    private LocalDate visitDate;
    private UUID projectId;
    private String vehicleType;
    private BigDecimal expenseAmount;
    private String remarks;
    private List<UUID> customerIds;
}
