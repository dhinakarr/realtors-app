package com.realtors.projects.services;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

@Service
public class PlotPricingService {

    public BigDecimal calculateArea(BigDecimal width, BigDecimal breadth) {
        return width.multiply(breadth);
    }

    public BigDecimal calculateBasePrice(BigDecimal area, BigDecimal perSqft) {
        return area.multiply(perSqft);
    }
}

