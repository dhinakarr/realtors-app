package com.realtors.sitevisit.dto;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class SitePaymentDTO {
	private UUID payment_id;
    private UUID userId;
    private BigDecimal amount;
    private String paymentMode;   // CASH / UPI / BANK
    private LocalDate paymentDate;
    private String remarks;
}
