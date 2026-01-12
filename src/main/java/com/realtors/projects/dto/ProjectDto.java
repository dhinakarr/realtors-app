package com.realtors.projects.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;

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

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;   // ✅ FIXED
    private Integer plotStartNumber;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;     // ✅ FIXED (nullable)
    private String plotNumbers;
    private Integer noOfPlots;
    private BigDecimal pricePerSqft;
    private BigDecimal regCharges;
    private BigDecimal docCharges;
    private BigDecimal otherCharges;
    private BigDecimal guidanceValue;
    private String status;
}
