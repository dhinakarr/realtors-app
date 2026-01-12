package com.realtors.sales.dto;

import lombok.Data;

@Data
public class CancelRequest {
	String reason;
	String refundType;
}
