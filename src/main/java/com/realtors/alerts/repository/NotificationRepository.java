package com.realtors.alerts.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.realtors.alerts.dto.NotificationDto;
import com.realtors.alerts.dto.NotificationType;
import com.realtors.alerts.rowmapper.NotificationRowMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class NotificationRepository {

	private final JdbcTemplate jdbc;

	public void save(Long userId, String title, String message, NotificationType type, Long referenceId) {

		jdbc.update("""
				    INSERT INTO notifications
				    (user_id, title, message, type, reference_id)
				    VALUES (?, ?, ?, ?, ?)
				""", userId, title, message, type.name(), referenceId);
	}

	public List<NotificationDto> findByUser(UUID userId) {

		return jdbc.query("""
				    SELECT notification_id, title, message, type, is_read, created_at
				    FROM notifications
				    WHERE user_id = ?
				    ORDER BY created_at DESC
				""", new NotificationRowMapper(), userId);
	}

	public void markAsRead(Long notificationId) {
		jdbc.update("""
				    UPDATE notifications
				    SET is_read = true
				    WHERE notification_id = ?
				""", notificationId);
	}

	public long unreadCount(UUID userId) {
		return jdbc.queryForObject("""
				    SELECT COUNT(*)
				    FROM notifications
				    WHERE user_id = ? AND is_read = false
				""", Long.class, userId);
	}
}
