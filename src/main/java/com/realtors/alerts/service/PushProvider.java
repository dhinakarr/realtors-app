package com.realtors.alerts.service;

import com.realtors.alerts.dto.PushMessage;

public interface PushProvider {
    void sendToUser(Long userId, PushMessage message);
}

