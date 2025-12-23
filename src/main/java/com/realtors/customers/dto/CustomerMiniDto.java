package com.realtors.customers.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerMiniDto {
    private UUID customerId;
    private String customerName;
    private Long mobile;
    private UUID soldBy;
}