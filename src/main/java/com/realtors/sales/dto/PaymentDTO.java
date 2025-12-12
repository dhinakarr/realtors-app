package com.realtors.sales.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Data;

@Data
public class PaymentDTO {
    private UUID paymentId;
    private UUID saleId;
    private BigDecimal amount;
    private LocalDateTime paymentDate;
    private String paymentMode;
    private String transactionRef;
    private String remarks;
}
