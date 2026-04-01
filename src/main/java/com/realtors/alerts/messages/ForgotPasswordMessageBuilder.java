package com.realtors.alerts.messages;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.realtors.admin.dto.RoleType;
import com.realtors.alerts.domain.event.EventType;
import com.realtors.alerts.domain.event.ForgotPasswordEvent;
import com.realtors.alerts.domain.notification.NotificationChannel;
import com.realtors.alerts.dto.NotificationInstruction;
import com.realtors.alerts.dto.RecipientDetail;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ForgotPasswordMessageBuilder {

	@Value("${app.frontend.base-url}")
	private String frontendBaseUrl;

	private static final Logger logger = LoggerFactory.getLogger(ForgotPasswordMessageBuilder.class);

	public List<NotificationInstruction> build(ForgotPasswordEvent event) {
		return List.of(customerMessage(event));
	}

	private NotificationInstruction customerMessage(ForgotPasswordEvent event) {
		RecipientDetail recipient = new RecipientDetail(event.getUserId(), event.getEmail(), null);
		return new NotificationInstruction(RoleType.CUSTOMER, recipient, event.getEventId(), event.getEventType(),
				List.of(new NotificationMessage(NotificationChannel.EMAIL, "Reset Your Password", null,
						buildEmailContext(event))));
	}

	private Map<String, Object> buildEmailContext(ForgotPasswordEvent data) {
		Map<String, Object> ctx = new HashMap<>();

		ctx.put("eventType", EventType.FORGOT_PASSWORD.name());
		ctx.put("template", "email/Forgot_Password"); // ✅ FIXED
		ctx.put("customerName", data.getUsername() != null ? data.getUsername() : "User");
		ctx.put("link", frontendBaseUrl + "/reset-password?token=" + data.getHashToken() // ✅ CRITICAL FIX
		);
		ctx.put("logoUrl", frontendBaseUrl + "/logo.png");
		ctx.put("year", LocalDate.now().getYear());
		return ctx;
	}
}
