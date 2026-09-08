package com.tezza.lending.notification.internal.kafka;

import com.tezza.lending.notification.internal.service.NotificationDispatchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class LoanEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(LoanEventConsumer.class);
    private final NotificationDispatchService dispatchService;

    public LoanEventConsumer(NotificationDispatchService dispatchService) {
        this.dispatchService = dispatchService;
    }

    @KafkaListener(
            topics = KafkaConfig.TOPIC_NOTIFICATIONS,
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory")
    public void consume(LoanEventMessage message) {
        log.info("Received notification event: {} for loan {}", message.getEventType(), message.getLoanNumber());
        try {
            dispatchService.dispatch(message);
        } catch (Exception e) {
            log.error("Error processing notification event {}: {}", message.getEventType(), e.getMessage(), e);
        }
    }
}
