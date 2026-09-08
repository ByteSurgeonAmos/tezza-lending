package com.tezza.lending.notification.internal.dispatcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SmsDispatcher {

    private static final Logger log = LoggerFactory.getLogger(SmsDispatcher.class);

    public void send(String phoneNumber, String body) {
        log.info("[SMS STUB] To: {} | Message: {}", phoneNumber, body);
    }
}
