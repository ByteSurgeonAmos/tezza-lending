package com.tezza.lending.notification.internal.entity;

import com.tezza.lending.notification.internal.entity.enums.EventType;
import com.tezza.lending.notification.internal.entity.enums.NotificationChannel;
import com.tezza.lending.shared.BaseAuditEntity;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "NOTIFICATION_TEMPLATES")
public class NotificationTemplate extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID")
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "EVENT_TYPE", nullable = false, length = 50)
    private EventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "CHANNEL", nullable = false, length = 10)
    private NotificationChannel channel;

    @Column(name = "SUBJECT", length = 200)
    private String subject;

    @Column(name = "BODY_TEMPLATE", nullable = false, columnDefinition = "TEXT")
    private String bodyTemplate;

    @Column(name = "ACTIVE", nullable = false)
    private boolean active = true;

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
