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
public class ExpenseDTO {

    private UUID expenseTypeId;
    private BigDecimal amount;
    private String paidBy;        // AGENT / COMPANY
    private LocalDate expenseDate;
    private String billReference;
    private String remarks;
}
