package com.realtors.alerts.repository;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.realtors.alerts.domain.notification.NotificationStatus;
import com.realtors.alerts.dto.NotificationInstruction;
import com.realtors.alerts.dto.NotificationResponse;
import com.realtors.alerts.messages.NotificationMessage;

@Repository
public class NotificationRepository {

	private final JdbcTemplate jdbcTemplate;

	public NotificationRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public void saveSuccess(NotificationInstruction req, String channel, String recipient) {
		NotificationMessage msg = req.messages().getFirst();
		String body = msg.getBody() == null ? null : msg.getBody();
		String title = msg.getTitle() == null ? null : msg.getTitle();
		jdbcTemplate.update("""
				    INSERT INTO notification_log
				    (event_id, event_type, channel, recipient, title, message, status)
				    VALUES (?, ?, ?, ?, ?, ?, ?)
				""", req.eventId(), req.eventType(), channel, recipient, title, body,
				NotificationStatus.SENT.name());
	}

	public void saveFailure(NotificationInstruction req, String channel, String recipient, String reason) {
		jdbcTemplate.update("""
				    INSERT INTO notification_log
				    (event_id, event_type, channel, recipient, title, message, status, failure_reason)
				    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
				""", req.eventId(), req.eventType(), channel, recipient, req.messages().getFirst().toString(),
				NotificationStatus.FAILED.name(), reason);
	}

	public List<NotificationResponse> findByUser(String userId, int limit, int offset) {

		return jdbcTemplate.query("""
				    SELECT id, event_id, event_type, channel, title, message, read 
				    FROM notification_log where recipient=? and read=false
				    ORDER BY created_at DESC
				    LIMIT ? OFFSET ?
				""", new Object[] { userId, limit, offset }, (rs, i) -> {
			NotificationResponse dto = new NotificationResponse();
			dto.setId(rs.getLong("id"));
			dto.setEventId(rs.getString("event_id"));
			dto.setEventType(rs.getString("event_type"));
			dto.setChannel(rs.getString("channel"));
			dto.setTitle(rs.getString("title"));
			dto.setMessage(rs.getString("message"));
			dto.setRead(rs.getBoolean("read"));
			return dto;
		});
	}

	public Long countUnread(String userId) {
		return jdbcTemplate.queryForObject("""
				    SELECT COUNT(*)
				    FROM notification_log
				    WHERE recipient = ? AND read = false
				""", Long.class, userId);
	}

	public void markAsRead(Long notificationId, String userId) {
		jdbcTemplate.update("""
				    UPDATE notification_log
				    SET read = true
				    WHERE id = ? AND recipient = ?
				""", notificationId, userId);
	}

	public void markAllAsRead(String userId) {
		jdbcTemplate.update("""
				    UPDATE notification_log
				    SET read = true
				    WHERE recipient = ?
				""", userId);
	}
}
