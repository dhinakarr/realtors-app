package com.realtors.alerts.rowmapper;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

import com.realtors.alerts.dto.NotificationDto;
import com.realtors.alerts.dto.NotificationType;

public class NotificationRowMapper implements RowMapper<NotificationDto> {

    @Override
    public NotificationDto mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new NotificationDto(
            rs.getLong("notification_id"),
            rs.getString("title"),
            rs.getString("message"),
            NotificationType.valueOf(rs.getString("type")),
            rs.getBoolean("is_read"),
            rs.getTimestamp("created_at").toLocalDateTime()
        );
    }
}
