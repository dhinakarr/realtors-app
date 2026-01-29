package com.realtors.alerts.domain.event;

import com.realtors.dashboard.dto.SaleDetailDTO;

import lombok.Data;

@Data
public class SaleCreatedEvent extends DomainEvent{
	String saleId;
	String soldBy;
	SaleDetailDTO saleDetails;
	
	public SaleCreatedEvent(String initiatedBy, String saleId, String eventType,  String soldBy, SaleDetailDTO saleDetails) {
        super(initiatedBy, eventType);
        this.saleId = saleId;
        this.soldBy = soldBy;
        this.saleDetails = saleDetails;
    }
}
