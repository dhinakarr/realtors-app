package com.realtors.alerts.listener;

import com.realtors.alerts.domain.event.DomainEvent;

public interface EventListener<T extends DomainEvent> {
    void handle(T event);
}
