package com.realtors.sales.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CommissionRow(
	    UUID userId,
	    UUID roleId,
	    BigDecimal percentage
	) {}
