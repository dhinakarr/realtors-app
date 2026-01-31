package com.realtors.alerts.listener;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.realtors.alerts.domain.event.SaleCreatedEvent;
import com.realtors.alerts.messages.SaleCreatedMessageBuilder;
import com.realtors.alerts.service.NotificationService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SaleNotificationListener {

	private final NotificationService notificationService;
	private final SaleCreatedMessageBuilder messageBuilder;
	
	private static final Logger logger = LoggerFactory.getLogger(SaleNotificationListener.class);

	@org.springframework.context.event.EventListener
	@Async("alertsExecutor")
	public void handle(SaleCreatedEvent event) {
		logger.debug("@SaleNotificationListener.handle event: {}", event);
		notificationService.send(messageBuilder.build(event));
	}
}
