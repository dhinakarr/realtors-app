package com.realtors.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;
import java.sql.Timestamp;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FeatureDto {
    private UUID featureId;
    private UUID moduleId;
    private String featureName;
    private String description;
    private String url;
    private String status;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private UUID createdBy;
    private UUID updatedBy;
}
