package com.tezza.lending.notification.internal.repository;

import com.tezza.lending.notification.internal.entity.NotificationTemplate;
import com.tezza.lending.notification.internal.entity.enums.EventType;
import com.tezza.lending.notification.internal.entity.enums.NotificationChannel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, UUID> {
    Optional<NotificationTemplate> findByEventTypeAndChannelAndActive(
            EventType eventType, NotificationChannel channel, boolean active);
    List<NotificationTemplate> findByActive(boolean active);
}
