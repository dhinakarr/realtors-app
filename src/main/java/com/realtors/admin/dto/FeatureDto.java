package com.realtors.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.sql.Timestamp;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FeatureDto {
    private UUID featureId;
    private UUID moduleId;
    private String featureName;
    private String moduleName;
    private String description;
    private String url;
    private String status;
    private @JsonIgnore Timestamp createdAt;
    private @JsonIgnore Timestamp updatedAt;
    private @JsonIgnore UUID createdBy;
    private @JsonIgnore UUID updatedBy;
}
