package com.realtors.projects.dto;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PlotUnitDto {
	private UUID plotId;
	private UUID projectId;
	private String plotNumber;
	private BigDecimal area;
	private BigDecimal basePrice;
	private String roadWidth;
	private String surveyNum;
	private String facing;
	private BigDecimal width;
	private BigDecimal breath;
	private BigDecimal totalPrice;
	private Boolean isPrime;
	private String status;
	private UUID customerId;
	private String remarks;
	private UUID updatedBy;
	private Timestamp updated_at;
}
