package com.realtors.alerts.service;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.realtors.alerts.dto.NotificationType;
import com.realtors.alerts.dto.PushMessage;
import com.realtors.alerts.repository.NotificationRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class NotificationService {

	private final NotificationRepository repo;
	private final PushProvider pushProvider;

	public void notifyUser(Long userId, String title, String message, NotificationType type, Long referenceId) {

		// 1. Store notification
		repo.save(userId, title, message, type, referenceId);

		// 2. Push (best effort)
		pushProvider.sendToUser(userId, new PushMessage(title, message, Map.of("type", type.name())));
	}
}
