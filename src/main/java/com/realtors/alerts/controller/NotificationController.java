package com.realtors.alerts.controller;

import java.util.List;

import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.realtors.alerts.dto.NotificationResponse;
import com.realtors.alerts.repository.NotificationRepository;
import com.realtors.alerts.service.DeviceService;
import com.realtors.common.ApiResponse;
import com.realtors.dashboard.dto.UserPrincipalDto;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class NotificationController {

	private final DeviceService deviceService;
	private final NotificationRepository notificationRepository;
	private static final Logger logger = LoggerFactory.getLogger(NotificationController.class);

	// 1️⃣ Register device token (FCM)
	@PostMapping("/device")
	public ResponseEntity<Void> registerDevice(@AuthenticationPrincipal UserPrincipalDto user,
			@RequestBody DeviceRegisterRequest request) {
		logger.info("@NotificationController.registerDevice request: {}", request.toString());
		deviceService.register(user.getUserId(), request);
		return ResponseEntity.ok().build();
	}

	// 2️⃣ Get my notifications (Inbox)
	@GetMapping
	public ResponseEntity<ApiResponse<List<NotificationResponse>>> getMyNotifications(@AuthenticationPrincipal UserPrincipalDto user) {
		List<NotificationResponse> data = notificationRepository.findByUser(user.getUserId().toString(), 10, 0);
		return ResponseEntity.ok(ApiResponse.success("Alert messages fetched", data, org.springframework.http.HttpStatus.OK));
	}

	// 3️⃣ Mark notification as read
	@PutMapping("/{notificationId}/read")
	public ResponseEntity<Void> markAsRead(@AuthenticationPrincipal UserPrincipalDto user, @PathVariable Long notificationId) {
		notificationRepository.markAsRead(notificationId, user.getUserId().toString());
		return ResponseEntity.ok().build();
	}

	// 4️⃣ Unread count (for bell badge)
	@GetMapping("/unread-count")
	public ResponseEntity<ApiResponse<Long>> unreadCount(@AuthenticationPrincipal UserPrincipalDto user) {
		Long count = notificationRepository.countUnread(user.getUserId().toString());
		return ResponseEntity.ok(ApiResponse.success("Un-read messages count", count, org.springframework.http.HttpStatus.OK));
	}
	
	@PostMapping("/mark-all-read")
	public ResponseEntity<ApiResponse<Long>> markAllRead(@AuthenticationPrincipal UserPrincipalDto user) {
		notificationRepository.markAllAsRead(user.getUserId().toString());
		return ResponseEntity.ok(ApiResponse.success("Un-read messages count", null, org.springframework.http.HttpStatus.OK));
	}
}
