package com.realtors.sales.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Component;

import com.realtors.sales.dto.CommissionSpreadRuleDTO;

@Component
public class CommissionCalculator {

    public BigDecimal calculate(
    		CommissionSpreadRuleDTO rule,
            BigDecimal saleAmount,
            BigDecimal area) {

        return switch (rule.getCommissionType()) {
            case "PERCENTAGE" ->
                saleAmount
                    .multiply(rule.getCommissionValue())
                    .divide(BigDecimal.valueOf(100));

            case "AMOUNT_PER_SQFT" ->
            area.multiply(rule.getCommissionValue());

            case "FLAT" ->
                rule.getCommissionValue();

            default -> BigDecimal.ZERO;
        };
    }
}
