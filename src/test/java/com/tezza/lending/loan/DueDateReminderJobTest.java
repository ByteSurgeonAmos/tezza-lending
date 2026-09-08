package com.tezza.lending.loan;

import com.tezza.lending.loan.api.event.DueDateReminderEvent;
import com.tezza.lending.loan.internal.entity.Loan;
import com.tezza.lending.loan.internal.entity.enums.LoanStatus;
import com.tezza.lending.loan.internal.repository.LoanRepository;
import com.tezza.lending.loan.internal.scheduler.DueDateReminderJob;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DueDateReminderJobTest {

    @Mock LoanRepository loanRepository;
    @Mock ApplicationEventPublisher eventPublisher;
    @InjectMocks DueDateReminderJob dueDateReminderJob;

    @Test
    void runDueDateReminderSweep_loanDueIn3Days_eventPublished() {
        ReflectionTestUtils.setField(dueDateReminderJob, "daysBeforeDue", 3);
        LocalDate targetDate = LocalDate.now().plusDays(3);

        Loan loan = new Loan();
        loan.setId(UUID.randomUUID());
        loan.setCustomerId(UUID.randomUUID());
        loan.setLoanNumber("TZ-2026-00000001");
        loan.setStatus(LoanStatus.OPEN);
        loan.setDueDate(targetDate);
        loan.setOutstandingBalance(BigDecimal.valueOf(10000));
        loan.setInstallments(new ArrayList<>());

        when(loanRepository.findLoansDueOn(targetDate)).thenReturn(List.of(loan));

        dueDateReminderJob.runDueDateReminderSweep();

        ArgumentCaptor<DueDateReminderEvent> captor = ArgumentCaptor.forClass(DueDateReminderEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().loanId()).isEqualTo(loan.getId());
        assertThat(captor.getValue().dueDate()).isEqualTo(targetDate);
    }

    @Test
    void runDueDateReminderSweep_noLoansDueSoon_noEventsPublished() {
        ReflectionTestUtils.setField(dueDateReminderJob, "daysBeforeDue", 3);
        when(loanRepository.findLoansDueOn(any())).thenReturn(List.of());

        dueDateReminderJob.runDueDateReminderSweep();

        verify(eventPublisher, never()).publishEvent(any());
    }
}
