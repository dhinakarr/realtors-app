package com.realtors.alerts.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class NotificationResponse {
    private Long id;
    private String eventId;
    private String eventType;
    private String channel;
    private String title; 
    private String message;
    private boolean read;
    private LocalDateTime createdAt;
}
