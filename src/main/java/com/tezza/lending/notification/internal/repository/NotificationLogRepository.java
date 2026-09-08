package com.tezza.lending.notification.internal.repository;

import com.tezza.lending.notification.internal.entity.NotificationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, UUID> {
    List<NotificationLog> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);
}
