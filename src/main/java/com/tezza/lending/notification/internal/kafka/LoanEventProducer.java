package com.tezza.lending.notification.internal.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class LoanEventProducer {

    private static final Logger log = LoggerFactory.getLogger(LoanEventProducer.class);
    private final KafkaTemplate<String, LoanEventMessage> kafkaTemplate;

    public LoanEventProducer(KafkaTemplate<String, LoanEventMessage> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void send(LoanEventMessage message) {
        kafkaTemplate.send(KafkaConfig.TOPIC_NOTIFICATIONS, message.getLoanId().toString(), message)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send notification event for loan {}: {}", message.getLoanNumber(), ex.getMessage());
                    } else {
                        log.debug("Notification event sent for loan {} event {}", message.getLoanNumber(), message.getEventType());
                    }
                });
    }
}
