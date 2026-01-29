package com.realtors.alerts.messages;

import java.util.Map;

import com.realtors.alerts.domain.event.DomainEvent;
import com.realtors.alerts.domain.notification.NotificationChannel;

public interface NotificationMessageBuilder<T extends DomainEvent> {

    String supports(); // event type

    Map<NotificationChannel, NotificationMessage> build(T event);
}
