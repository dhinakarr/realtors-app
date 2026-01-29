package com.realtors.alerts.sender;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.realtors.alerts.domain.notification.NotificationChannel;
import com.realtors.alerts.dto.NotificationInstruction;
import com.realtors.alerts.exception.NotificationDeliveryException;
import com.realtors.alerts.messages.NotificationMessage;
import com.realtors.alerts.service.EmailService;
import com.realtors.alerts.service.EmailTemplateService;

@Component
public class EmailNotificationSender implements NotificationSender {
	private static final Logger logger = LoggerFactory.getLogger(EmailNotificationSender.class);

	private final EmailService emailService;
	private final EmailTemplateService templateService;

	public EmailNotificationSender(EmailService emailService, EmailTemplateService templateService) {
		this.emailService = emailService;
		this.templateService = templateService;
	}

	@Override
	public NotificationChannel channel() {
		return NotificationChannel.EMAIL;
	}

	@Override
	public void send(NotificationInstruction instruction, String recipient) {
		logger.info("Sending EMAIL notification to {}", recipient);
		
		NotificationMessage msg = instruction.messages().stream()
				.filter(m -> m.getChannel() == NotificationChannel.EMAIL).findFirst().orElseThrow();
		
		String html = templateService.buildNotificationEmail(msg.getTitle(), msg.getBody(), msg.getHtmlContent());
		try {
			emailService.sendHtmlEmail(recipient, msg.getTitle(), html);
		} catch (NotificationDeliveryException ex) {
			logger.error("@EmailNotificationSender.send Exception: {}", ex);
		}

	}
}
