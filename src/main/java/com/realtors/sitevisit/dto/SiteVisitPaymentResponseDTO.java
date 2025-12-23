package com.realtors.sitevisit.dto;

import java.util.List;

import lombok.Data;

@Data
public class SiteVisitPaymentResponseDTO {
	
	private PaymentSummaryDTO summary;
    private List<SitePaymentDTO> payments;

}
