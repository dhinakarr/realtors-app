package com.realtors.projects.services;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.realtors.common.util.AppUtil;

@Service
public class PlotPricingService {

    public BigDecimal calculateArea(BigDecimal width, BigDecimal breadth) {
        return width.multiply(AppUtil.nz(breadth));
    }

    public BigDecimal calculateBasePrice(BigDecimal area, BigDecimal perSqft) {
        return area.multiply(AppUtil.nz(perSqft));
    }
}

