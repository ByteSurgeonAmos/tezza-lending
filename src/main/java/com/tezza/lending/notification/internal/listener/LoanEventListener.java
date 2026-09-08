package com.tezza.lending.notification.internal.listener;

import com.tezza.lending.loan.api.DueDateReminderEvent;
import com.tezza.lending.loan.api.LoanCreatedEvent;
import com.tezza.lending.loan.api.LoanOverdueEvent;
import com.tezza.lending.loan.api.RepaymentReceivedEvent;
import com.tezza.lending.notification.internal.kafka.LoanEventMessage;
import com.tezza.lending.notification.internal.kafka.LoanEventProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class LoanEventListener {

    private static final Logger log = LoggerFactory.getLogger(LoanEventListener.class);
    private final LoanEventProducer producer;

    public LoanEventListener(LoanEventProducer producer) {
        this.producer = producer;
    }

    @EventListener
    @Async
    public void onLoanCreated(LoanCreatedEvent event) {
        LoanEventMessage message = LoanEventMessage.builder()
                .eventType("LOAN_CREATED")
                .loanId(event.loanId())
                .customerId(event.customerId())
                .loanNumber(event.loanNumber())
                .amount(event.amount())
                .dueDate(event.dueDate())
                .build();
        producer.send(message);
    }

    @EventListener
    @Async
    public void onLoanOverdue(LoanOverdueEvent event) {
        LoanEventMessage message = LoanEventMessage.builder()
                .eventType("OVERDUE_NOTICE")
                .loanId(event.loanId())
                .customerId(event.customerId())
                .loanNumber(event.loanNumber())
                .outstandingBalance(event.outstandingBalance())
                .build();
        producer.send(message);
    }

    @EventListener
    @Async
    public void onDueDateReminder(DueDateReminderEvent event) {
        LoanEventMessage message = LoanEventMessage.builder()
                .eventType("DUE_REMINDER")
                .loanId(event.loanId())
                .customerId(event.customerId())
                .loanNumber(event.loanNumber())
                .outstandingBalance(event.outstandingBalance())
                .dueDate(event.dueDate())
                .build();
        producer.send(message);
    }

    @EventListener
    @Async
    public void onRepaymentReceived(RepaymentReceivedEvent event) {
        LoanEventMessage message = LoanEventMessage.builder()
                .eventType("REPAYMENT_ACK")
                .loanId(event.loanId())
                .customerId(event.customerId())
                .loanNumber(event.loanNumber())
                .amount(event.amountPaid())
                .outstandingBalance(event.remainingBalance())
                .build();
        producer.send(message);
    }
}
