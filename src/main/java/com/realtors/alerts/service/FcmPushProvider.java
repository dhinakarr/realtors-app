package com.realtors.alerts.service;

import java.util.List;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import com.realtors.alerts.dto.PushMessage;
import com.realtors.alerts.repository.UserDeviceRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class FcmPushProvider implements PushProvider {

	private final UserDeviceRepository deviceRepo;

	@Override
	@Async
	public void sendToUser(Long userId, PushMessage message) {

		List<String> tokens = deviceRepo.findActiveTokens(userId);

		for (String token : tokens) {
			try {
				Message fcmMessage = Message.builder().setToken(token)
						.setNotification(
								Notification.builder().setTitle(message.getTitle()).setBody(message.getBody()).build())
						.putAllData(message.getData()).build();

				FirebaseMessaging.getInstance().send(fcmMessage);

			} catch (Exception ex) {
				// log only, never break business flow
			}
		}
	}
}
