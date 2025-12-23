package com.realtors.sitevisit.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.realtors.customers.dto.CustomerMiniDto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class SiteVisitResponseDTO {

    private UUID siteVisitId;
    private LocalDate visitDate;

    private UUID userId;
    private String userName;

    private UUID projectId;
    private String projectName;

    private String vehicleType;
    private BigDecimal expenseAmount;

    private BigDecimal totalPaid;
    private BigDecimal balance;
    private String status;
    private String remarks;
    private List<CustomerMiniDto> customers;
}


