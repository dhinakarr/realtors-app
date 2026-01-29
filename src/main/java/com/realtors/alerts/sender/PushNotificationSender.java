package com.realtors.alerts.sender;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.realtors.alerts.domain.notification.NotificationChannel;
import com.realtors.alerts.dto.NotificationInstruction;
import com.realtors.alerts.dto.RecipientDetail;
import com.realtors.alerts.messages.NotificationMessage;
import com.realtors.alerts.repository.NotificationRepository;
import com.realtors.alerts.repository.UserDeviceRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PushNotificationSender implements NotificationSender {

	private static final Logger logger = LoggerFactory.getLogger(PushNotificationSender.class);
	
	private final UserDeviceRepository deviceRepo;
	private final NotificationRepository notificationrepo;

	@Override
	public NotificationChannel channel() {
		return NotificationChannel.PUSH;
	}

	@Override
	public void send(NotificationInstruction request, String recipient) {
		
		List<String> tokens = deviceRepo.findActiveTokens(UUID.fromString(recipient));

		if (tokens.isEmpty()) {
			logger.warn("@PushNotificationSender.send No active FCM tokens for user {}", recipient);
			return;
		}
		String token = tokens.getFirst();
		NotificationMessage msg = request.messages().stream()
			    .filter(m -> m.getChannel() == NotificationChannel.PUSH)
			    .findFirst()
			    .orElseThrow();
		RecipientDetail reciever = request.recipient();
		Message message = Message.builder().setToken(token)
				.setNotification(Notification.builder().setTitle(msg.getTitle()).setBody(msg.getBody()).build())
				.putData("recipientId", reciever.userId().toString()).build();

		try {
			String response = FirebaseMessaging.getInstance().send(message);
			notificationrepo.saveSuccess(request, NotificationChannel.PUSH.name(), recipient);
			logger.info("@PushNotificationSender.send Push sent: {}", response);

		} catch (FirebaseMessagingException ex) {
			notificationrepo.saveFailure(request, NotificationChannel.PUSH.name(), recipient, ex.getMessage());
			throw new RuntimeException("Push failed", ex);
		}
	}
}
