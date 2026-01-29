package com.realtors.alerts.sender;

import com.realtors.alerts.domain.notification.NotificationChannel;
import com.realtors.alerts.dto.NotificationInstruction;

public interface NotificationSender {
    NotificationChannel channel();
    void send(NotificationInstruction  request, String recipient);
}
