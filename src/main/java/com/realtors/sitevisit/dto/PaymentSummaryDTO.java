package com.realtors.sitevisit.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class PaymentSummaryDTO {

    private BigDecimal totalExpense;
    private BigDecimal totalPaid;
    private BigDecimal balanceAmount;

    // getters & setters
}
