package com.realtors.alerts.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Data;

@Data
public abstract class DomainEvent {
    private String eventId;
    private String eventType;
    private String initiatedBy;
    private LocalDateTime occurredAt;
    
    protected DomainEvent(String initiatedBy, String eventType) {
        this.eventId = UUID.randomUUID().toString();
        this.initiatedBy = initiatedBy;
        this.eventType = eventType;
        this.occurredAt = LocalDateTime.now();
    }
    
    public String getEventId() { return eventId; }
    public String getInitiatedBy() { return initiatedBy; }
    public String getEventtype() {return eventType;};
    public LocalDateTime getOccurredAt() { return occurredAt; }
}
