package com.realtors.sitevisit.dto;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class VehicleUsageDTO {

    private UUID vehicleId;

    private BigDecimal fuelCost;
    private BigDecimal driverCost;
    private BigDecimal tollCost;
    private BigDecimal rentCost;
}
