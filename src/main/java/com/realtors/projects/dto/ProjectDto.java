package com.realtors.projects.dto;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectDto {
    private UUID projectId;
    private String projectName;
    private String locationDetails;
    private String surveyNumber;
    private Date startDate;
    private Date endDate;
    private Integer noOfPlots;
    private Integer plotStartNumber;
    private BigDecimal pricePerSqft;
    private BigDecimal regCharges;
    private BigDecimal docCharges;
    private BigDecimal otherCharges;
    private BigDecimal guidanceValue;
    private String status;
}

