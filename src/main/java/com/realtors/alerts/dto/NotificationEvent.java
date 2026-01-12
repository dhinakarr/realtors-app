package com.realtors.alerts.dto;

public record NotificationEvent(
	    Long userId,
	    String title,
	    String message,
	    NotificationType type
	) {}
