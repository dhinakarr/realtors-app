package com.realtors.dashboard.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class CommissionDashboardDTO {
    private CommissionSummaryDTO summary;
    private List<CommissionRowDTO> commissions;
}
