package com.tezza.lending.loan.internal.scheduler;

import com.tezza.lending.loan.api.event.DueDateReminderEvent;
import com.tezza.lending.loan.internal.entity.Loan;
import com.tezza.lending.loan.internal.repository.LoanRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
public class DueDateReminderJob {

    private static final Logger log = LoggerFactory.getLogger(DueDateReminderJob.class);
    private final LoanRepository loanRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${tezza.notification.due-reminder-days-before:3}")
    private int daysBeforeDue;

    public DueDateReminderJob(LoanRepository loanRepository, ApplicationEventPublisher eventPublisher) {
        this.loanRepository = loanRepository;
        this.eventPublisher = eventPublisher;
    }

    @Scheduled(cron = "0 0 8 * * *")
    @Transactional(readOnly = true)
    public void runDueDateReminderSweep() {
        LocalDate targetDate = LocalDate.now().plusDays(daysBeforeDue);
        List<Loan> loans = loanRepository.findLoansDueOn(targetDate);
        log.info("DueDateReminderJob: sending reminders for {} loans due on {}", loans.size(), targetDate);

        for (Loan loan : loans) {
            eventPublisher.publishEvent(new DueDateReminderEvent(
                    loan.getId(), loan.getCustomerId(),
                    loan.getLoanNumber(), loan.getOutstandingBalance(), loan.getDueDate()));
        }
    }
}
