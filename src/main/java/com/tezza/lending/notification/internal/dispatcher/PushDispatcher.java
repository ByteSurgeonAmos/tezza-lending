package com.tezza.lending.notification.internal.dispatcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class PushDispatcher {

    private static final Logger log = LoggerFactory.getLogger(PushDispatcher.class);

    public void send(UUID customerId, String title, String body) {
        log.info("[PUSH STUB] CustomerId: {} | Title: {} | Body: {}", customerId, title, body);
    }
}
