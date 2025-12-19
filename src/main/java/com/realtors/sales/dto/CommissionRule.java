package com.realtors.sales.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CommissionRule(
        UUID roleId,
        int roleLevel,
        BigDecimal percentage
) {}

