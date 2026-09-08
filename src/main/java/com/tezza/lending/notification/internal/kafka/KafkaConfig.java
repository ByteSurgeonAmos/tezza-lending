package com.tezza.lending.notification.internal.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    public static final String TOPIC_NOTIFICATIONS = "lending.notifications";
    public static final String TOPIC_LOAN_EVENTS = "lending.loan-events";

    @Bean
    public NewTopic notificationsTopic() {
        return TopicBuilder.name(TOPIC_NOTIFICATIONS).partitions(3).replicas(1).build();
    }

    @Bean
    public NewTopic loanEventsTopic() {
        return TopicBuilder.name(TOPIC_LOAN_EVENTS).partitions(3).replicas(1).build();
    }
}
