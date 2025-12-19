package com.realtors.sales.dto;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStageDTO {
	
	UUID stageId; 
	UUID projectId; 
    String stageName;
    String description;
    BigDecimal percentage;
    Integer sequence;
    Timestamp createdAt ;

}
