package com.tezza.lending.notification.api.dto;

import com.tezza.lending.notification.internal.entity.NotificationTemplate;
import com.tezza.lending.notification.internal.entity.enums.EventType;
import com.tezza.lending.notification.internal.entity.enums.NotificationChannel;

import java.util.UUID;

public class TemplateResponse {
    private UUID id;
    private EventType eventType;
    private NotificationChannel channel;
    private String subject;
    private String bodyTemplate;
    private boolean active;

    public static TemplateResponse from(NotificationTemplate t) {
        TemplateResponse r = new TemplateResponse();
        r.setId(t.getId());
        r.setEventType(t.getEventType());
        r.setChannel(t.getChannel());
        r.setSubject(t.getSubject());
        r.setBodyTemplate(t.getBodyTemplate());
        r.setActive(t.isActive());
        return r;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public EventType getEventType() { return eventType; }
    public void setEventType(EventType eventType) { this.eventType = eventType; }
    public NotificationChannel getChannel() { return channel; }
    public void setChannel(NotificationChannel channel) { this.channel = channel; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getBodyTemplate() { return bodyTemplate; }
    public void setBodyTemplate(String bodyTemplate) { this.bodyTemplate = bodyTemplate; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
