package com.realtors.sitevisit.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class PaymentPatchDTO {

    private BigDecimal amount;
    private String paymentMode;
    private String remarks;
}
