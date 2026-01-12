package com.realtors.projects.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class PlotDetailsDto {
	BigDecimal documentationCharges;
	BigDecimal otherCharges;
	BigDecimal ratePerSqft;
	PlotUnitDto plotData;

}
