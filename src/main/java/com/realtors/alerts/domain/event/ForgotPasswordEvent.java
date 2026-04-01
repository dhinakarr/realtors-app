package com.realtors.alerts.domain.event;

import java.util.UUID;

import lombok.Data;

@Data
public class ForgotPasswordEvent extends DomainEvent {
	private UUID userId;
	private String email; 
	private String username;
	private String hashToken;
	
	public ForgotPasswordEvent(String initiatedBy, String eventType, String email, String username, String hashToken, UUID userId) {
		super(initiatedBy, eventType);
		this.email = email;
		this.username = username;
		this.hashToken = hashToken;
		this.userId = userId;
	}
}
