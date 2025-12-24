package com.realtors.dashboard.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class ReceivableDashboardDTO {
    private ReceivableSummaryDTO summary;
    private List<ReceivableDetailDTO> receivables;
}
