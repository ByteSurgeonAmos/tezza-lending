package com.tezza.lending.notification.api.dto;

import com.tezza.lending.notification.internal.entity.NotificationLog;
import com.tezza.lending.notification.internal.entity.enums.NotificationChannel;
import com.tezza.lending.notification.internal.entity.enums.NotificationStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public class NotificationLogResponse {
    private UUID id;
    private UUID customerId;
    private String eventType;
    private NotificationChannel channel;
    private String recipient;
    private NotificationStatus status;
    private String errorMessage;
    private LocalDateTime sentAt;

    public static NotificationLogResponse from(NotificationLog l) {
        NotificationLogResponse r = new NotificationLogResponse();
        r.setId(l.getId());
        r.setCustomerId(l.getCustomerId());
        r.setEventType(l.getEventType());
        r.setChannel(l.getChannel());
        r.setRecipient(l.getRecipient());
        r.setStatus(l.getStatus());
        r.setErrorMessage(l.getErrorMessage());
        r.setSentAt(l.getSentAt());
        return r;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public NotificationChannel getChannel() { return channel; }
    public void setChannel(NotificationChannel channel) { this.channel = channel; }
    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }
    public NotificationStatus getStatus() { return status; }
    public void setStatus(NotificationStatus status) { this.status = status; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
}
