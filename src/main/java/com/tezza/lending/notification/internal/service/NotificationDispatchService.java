package com.tezza.lending.notification.internal.service;

import com.tezza.lending.notification.internal.dispatcher.EmailDispatcher;
import com.tezza.lending.notification.internal.dispatcher.PushDispatcher;
import com.tezza.lending.notification.internal.dispatcher.SmsDispatcher;
import com.tezza.lending.notification.internal.entity.NotificationLog;
import com.tezza.lending.notification.internal.entity.NotificationRule;
import com.tezza.lending.notification.internal.entity.NotificationTemplate;
import com.tezza.lending.notification.internal.entity.enums.EventType;
import com.tezza.lending.notification.internal.entity.enums.NotificationChannel;
import com.tezza.lending.notification.internal.entity.enums.NotificationStatus;
import com.tezza.lending.notification.internal.kafka.LoanEventMessage;
import com.tezza.lending.notification.internal.repository.NotificationLogRepository;
import com.tezza.lending.notification.internal.repository.NotificationRuleRepository;
import com.tezza.lending.notification.internal.repository.NotificationTemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class NotificationDispatchService {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatchService.class);
    private final NotificationRuleRepository ruleRepository;
    private final NotificationTemplateRepository templateRepository;
    private final NotificationLogRepository logRepository;
    private final TemplateRendererService templateRenderer;
    private final EmailDispatcher emailDispatcher;
    private final SmsDispatcher smsDispatcher;
    private final PushDispatcher pushDispatcher;

    public NotificationDispatchService(NotificationRuleRepository ruleRepository,
                                       NotificationTemplateRepository templateRepository,
                                       NotificationLogRepository logRepository,
                                       TemplateRendererService templateRenderer,
                                       EmailDispatcher emailDispatcher,
                                       SmsDispatcher smsDispatcher,
                                       PushDispatcher pushDispatcher) {
        this.ruleRepository = ruleRepository;
        this.templateRepository = templateRepository;
        this.logRepository = logRepository;
        this.templateRenderer = templateRenderer;
        this.emailDispatcher = emailDispatcher;
        this.smsDispatcher = smsDispatcher;
        this.pushDispatcher = pushDispatcher;
    }

    public void dispatch(LoanEventMessage message) {
        EventType eventType;
        try {
            eventType = EventType.valueOf(message.getEventType());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown event type: {}", message.getEventType());
            return;
        }

        List<NotificationRule> rules = ruleRepository.findByEventTypeAndEnabledOrderByPriorityAsc(
                message.getEventType(), true);

        Map<String, String> vars = buildVariables(message);

        for (NotificationRule rule : rules) {
            if (rule.getProductId() != null) continue;

            Optional<NotificationTemplate> templateOpt = templateRepository
                    .findByEventTypeAndChannelAndActive(eventType, rule.getChannel(), true);

            if (templateOpt.isEmpty()) {
                log.debug("No active template for event {} channel {}", eventType, rule.getChannel());
                continue;
            }

            NotificationTemplate template = templateOpt.get();
            String renderedBody = templateRenderer.render(template.getBodyTemplate(), vars);
            String renderedSubject = templateRenderer.render(
                    template.getSubject() != null ? template.getSubject() : "", vars);

            NotificationLog notifLog = new NotificationLog();
            notifLog.setCustomerId(message.getCustomerId());
            notifLog.setEventType(message.getEventType());
            notifLog.setChannel(rule.getChannel());
            notifLog.setRecipient(resolveRecipient(rule.getChannel(), message));
            notifLog.setStatus(NotificationStatus.PENDING);

            try {
                sendViaChannel(rule.getChannel(), message, renderedSubject, renderedBody);
                notifLog.setStatus(NotificationStatus.SENT);
                notifLog.setSentAt(LocalDateTime.now());
                log.info("Notification sent: {} via {} to {}", eventType, rule.getChannel(), notifLog.getRecipient());
            } catch (Exception e) {
                notifLog.setStatus(NotificationStatus.FAILED);
                notifLog.setErrorMessage(e.getMessage());
                log.error("Notification failed: {} via {} — {}", eventType, rule.getChannel(), e.getMessage());
            }
            logRepository.save(notifLog);
        }
    }

    private void sendViaChannel(NotificationChannel channel, LoanEventMessage message,
                                 String subject, String body) {
        switch (channel) {
            case EMAIL -> emailDispatcher.send(message.getCustomerEmail(), subject, body);
            case SMS -> smsDispatcher.send(message.getCustomerPhone(), body);
            case PUSH -> pushDispatcher.send(message.getCustomerId(), subject, body);
        }
    }

    private String resolveRecipient(NotificationChannel channel, LoanEventMessage message) {
        return switch (channel) {
            case EMAIL -> message.getCustomerEmail() != null ? message.getCustomerEmail() : "unknown";
            case SMS -> message.getCustomerPhone() != null ? message.getCustomerPhone() : "unknown";
            case PUSH -> message.getCustomerId().toString();
        };
    }

    private Map<String, String> buildVariables(LoanEventMessage message) {
        return Map.of(
                "customerName", message.getCustomerName() != null ? message.getCustomerName() : "",
                "loanNumber", message.getLoanNumber() != null ? message.getLoanNumber() : "",
                "amount", message.getAmount() != null ? message.getAmount().toPlainString() : "0",
                "balance", message.getOutstandingBalance() != null ? message.getOutstandingBalance().toPlainString() : "0",
                "dueDate", message.getDueDate() != null ? message.getDueDate().toString() : ""
        );
    }
}
