package com.tezza.lending.notification.internal.entity;

import com.tezza.lending.notification.internal.entity.enums.NotificationChannel;
import com.tezza.lending.shared.BaseAuditEntity;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "NOTIFICATION_RULES")
public class NotificationRule extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID")
    private UUID id;

    @Column(name = "PRODUCT_ID")
    private UUID productId;

    @Column(name = "CUSTOMER_SEGMENT", length = 50)
    private String customerSegment;

    @Column(name = "EVENT_TYPE", nullable = false, length = 50)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "CHANNEL", nullable = false, length = 10)
    private NotificationChannel channel;

    @Column(name = "ENABLED", nullable = false)
    private boolean enabled = true;

    @Column(name = "PRIORITY", nullable = false)
    private int priority = 0;

    @Column(name = "DELAY_MINUTES", nullable = false)
    private int delayMinutes = 0;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }
    public String getCustomerSegment() { return customerSegment; }
    public void setCustomerSegment(String customerSegment) { this.customerSegment = customerSegment; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public NotificationChannel getChannel() { return channel; }
    public void setChannel(NotificationChannel channel) { this.channel = channel; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }
    public int getDelayMinutes() { return delayMinutes; }
    public void setDelayMinutes(int delayMinutes) { this.delayMinutes = delayMinutes; }
}
