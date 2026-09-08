package com.tezza.lending.notification.api.dto;

import com.tezza.lending.notification.internal.entity.enums.EventType;
import com.tezza.lending.notification.internal.entity.enums.NotificationChannel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class TemplateRequest {
    @NotNull private EventType eventType;
    @NotNull private NotificationChannel channel;
    private String subject;
    @NotBlank private String bodyTemplate;

    public EventType getEventType() { return eventType; }
    public void setEventType(EventType eventType) { this.eventType = eventType; }
    public NotificationChannel getChannel() { return channel; }
    public void setChannel(NotificationChannel channel) { this.channel = channel; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getBodyTemplate() { return bodyTemplate; }
    public void setBodyTemplate(String bodyTemplate) { this.bodyTemplate = bodyTemplate; }
}
