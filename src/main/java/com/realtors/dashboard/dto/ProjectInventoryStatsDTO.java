package com.realtors.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import java.util.UUID;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class ProjectInventoryStatsDTO {
    private UUID projectId;
    private String projectName;
    private String status;
    private Long count;
}
