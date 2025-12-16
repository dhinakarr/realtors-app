package com.realtors.sales.dto;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SaleWiseReceivable {
	UUID saleId;
	BigDecimal totalPrice;
	BigDecimal totalReceived;
	BigDecimal totalReceivable;
}
