package com.realtors.alerts.messages;

import java.util.Map;

import com.realtors.alerts.domain.notification.NotificationChannel;

import lombok.Data;

@Data
public class NotificationMessage {
    private String title;
    private String body;
    NotificationChannel channel;
    Map<String, Object> htmlContent;
    public NotificationMessage(NotificationChannel channel, String title, String body, Map<String, Object> htmlContent) {
    	this.title = title;
    	this.body = body;
    	this.channel = channel;
    	this.htmlContent = htmlContent;
    }
}
