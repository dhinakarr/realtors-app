package com.realtors.projects.dto;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectSummaryDto {
	    UUID projectId;
	    String projectName;
	    String locationDetails;
	    String surveyNumber;
	    Date startDate;
	    Date endDate;
	    String plotNumbers;
	    Integer noOfPlots;
	    Integer plotStartNumber;
	    BigDecimal pricePerSqft;
	    BigDecimal regCharges;
	    BigDecimal docCharges;
	    BigDecimal otherCharges;
	    BigDecimal guidanceValue;
	    Instant createdAt;
	    Instant updatedAt;
	    String status;

	    // This is the key: files belong to the project
	    List<ProjectFileDto> files  = new ArrayList<>();  // ← 0 or 1 when no filter, 0..n when filtered
	    private List<ProjectDocumentDto> documents = new ArrayList<>();
}