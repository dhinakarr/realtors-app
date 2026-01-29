package com.realtors.alerts.dto;

import java.util.List;

import com.realtors.admin.dto.RoleType;
import com.realtors.alerts.messages.NotificationMessage;

public record NotificationInstruction(
	    RoleType stakeholder,
	    RecipientDetail recipient,
	    String eventId,
	    String eventType,
	    List<NotificationMessage> messages
	) {}
