package com.realtors.sales.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Data;

@Data
public class PaymentDTO {
    private UUID paymentId;
    private UUID saleId;
    private UUID plotId;
    private String paymentType;
    private BigDecimal amount;
    private LocalDateTime paymentDate;
    private String paymentMode;
    private UUID collectedBy;
    private UUID paidTo;
    private String transactionRef;
    private String remarks;
    private boolean verified; 
}
