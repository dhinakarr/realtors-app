package com.realtors.alerts.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.realtors.alerts.dto.DevicePlatform;
import com.realtors.alerts.rowmapper.DeviceTokenRowMapper;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class UserDeviceRepository {

	private final JdbcTemplate jdbc;

	public List<String> findActiveTokens(UUID userId) {
		return jdbc.query("""
				    SELECT device_token
				    FROM user_devices
				    WHERE user_id = ? AND active = true
				""", new DeviceTokenRowMapper(), userId);
	}

	public Optional<Long> findDeviceIdByToken(String token) {

		List<Long> ids = jdbc.query("""
				    SELECT device_id
				    FROM user_devices
				    WHERE device_token = ?
				""", (rs, i) -> rs.getLong("device_id"), token);

		return ids.stream().findFirst();
	}

	public void saveDevice(UUID userId, String token, DevicePlatform platform) {
		jdbc.update("""
				    INSERT INTO user_devices (user_id, device_token, platform)
				    VALUES (?, ?, ?)
				""", userId, token, platform.name());
	}

	public void touchDevice(Long deviceId) {
		jdbc.update("""
				    UPDATE user_devices
				    SET active = true, last_used_at = NOW()
				    WHERE device_id = ?
				""", deviceId);
	}
}
