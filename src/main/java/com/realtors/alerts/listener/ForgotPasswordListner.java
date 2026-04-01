package com.realtors.alerts.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.realtors.alerts.domain.event.ForgotPasswordEvent;
import com.realtors.alerts.messages.ForgotPasswordMessageBuilder;
import com.realtors.alerts.service.NotificationService;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ForgotPasswordListner {

	private static final Logger logger = LoggerFactory.getLogger(ForgotPasswordListner.class);
	
	private final NotificationService notificationService;
	private final ForgotPasswordMessageBuilder messageBuilder;
	

	@org.springframework.context.event.EventListener
	@Async("alertsExecutor")
	public void handle(ForgotPasswordEvent event) {
		notificationService.send(messageBuilder.build(event));
	}
}
