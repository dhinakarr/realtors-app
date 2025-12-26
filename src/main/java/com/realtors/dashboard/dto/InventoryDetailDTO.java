package com.realtors.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class InventoryDetailDTO {

    private String status;   // AVAILABLE, BOOKED, SOLD...
    private Long count;

    // getters & setters
}

