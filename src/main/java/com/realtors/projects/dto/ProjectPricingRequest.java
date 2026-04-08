package com.realtors.projects.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class ProjectPricingRequest {
    private BigDecimal guidanceValue;
    private BigDecimal pricePerSqft;

    // getters & setters
}
