package com.realtors.sales.dto;

import java.math.BigDecimal;
import java.util.UUID;


public record SaleContext(
        UUID saleId,
        UUID projectId,
        UUID sellerUserId,
        UUID sellerRoleId,
        int sellerRoleLevel,
        BigDecimal saleAmount
) {}


