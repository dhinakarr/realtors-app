package com.realtors.alerts.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.realtors.alerts.dto.DeviceRegisterRequest;
import com.realtors.alerts.dto.NotificationDto;
import com.realtors.alerts.repository.NotificationRepository;
import com.realtors.alerts.service.DeviceService;
import com.realtors.dashboard.dto.UserPrincipalDto;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class NotificationController {

	private final DeviceService deviceService;
	private final NotificationRepository notificationRepository;

	// 1️⃣ Register device token (FCM)
	@PostMapping("/device")
	public ResponseEntity<Void> registerDevice(@AuthenticationPrincipal UserPrincipalDto user,
			@RequestBody DeviceRegisterRequest request) {
		deviceService.register(user.getUserId(), request);
		return ResponseEntity.ok().build();
	}

	// 2️⃣ Get my notifications (Inbox)
	@GetMapping
	public ResponseEntity<List<NotificationDto>> getMyNotifications(@AuthenticationPrincipal UserPrincipalDto user) {
		return ResponseEntity.ok(notificationRepository.findByUser(user.getUserId()));
	}

	// 3️⃣ Mark notification as read
	@PutMapping("/{notificationId}/read")
	public ResponseEntity<Void> markAsRead(@PathVariable Long notificationId) {
		notificationRepository.markAsRead(notificationId);
		return ResponseEntity.ok().build();
	}

	// 4️⃣ Unread count (for bell badge)
	@GetMapping("/unread-count")
	public ResponseEntity<Long> unreadCount(@AuthenticationPrincipal UserPrincipalDto user) {
		return ResponseEntity.ok(notificationRepository.unreadCount(user.getUserId()));
	}
}
