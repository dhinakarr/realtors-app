package com.realtors.alerts.domain.event;

import lombok.Data;

@Data
public class ChangePasswordEvent extends DomainEvent {
	private String email; 
	private String username;
	
	public ChangePasswordEvent(String initiatedBy, String eventType) {
		super(initiatedBy, eventType);
	}
}
