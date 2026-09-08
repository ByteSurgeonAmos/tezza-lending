package com.tezza.lending.loan;

import com.tezza.lending.loan.api.event.LoanOverdueEvent;
import com.tezza.lending.loan.internal.entity.Loan;
import com.tezza.lending.loan.internal.entity.enums.LoanStatus;
import com.tezza.lending.loan.internal.repository.LoanRepository;
import com.tezza.lending.loan.internal.scheduler.OverdueSweepJob;
import com.tezza.lending.loan.internal.service.FeeCalculatorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OverdueSweepJobTest {

    @Mock LoanRepository loanRepository;
    @Mock FeeCalculatorService feeCalculatorService;
    @Mock ApplicationEventPublisher eventPublisher;
    @InjectMocks OverdueSweepJob overdueSweepJob;

    @Test
    void runOverdueSweep_overdueLoans_markedOverdueAndEventPublished() {
        Loan loan = buildLoan("TZ-2026-00000001", LoanStatus.OPEN, LocalDate.now().minusDays(5), BigDecimal.valueOf(10000));

        when(loanRepository.findOverdueLoans(any())).thenReturn(List.of(loan));
        when(feeCalculatorService.calculateLateFees(any(), any(), anyInt())).thenReturn(BigDecimal.valueOf(500));
        when(loanRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        overdueSweepJob.runOverdueSweep();

        assertThat(loan.getStatus()).isEqualTo(LoanStatus.OVERDUE);
        assertThat(loan.getOutstandingBalance()).isEqualByComparingTo("10500.00");

        ArgumentCaptor<LoanOverdueEvent> captor = ArgumentCaptor.forClass(LoanOverdueEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().loanId()).isEqualTo(loan.getId());
    }

    @Test
    void runOverdueSweep_noOverdueLoans_noEventsPublished() {
        when(loanRepository.findOverdueLoans(any())).thenReturn(List.of());

        overdueSweepJob.runOverdueSweep();

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void runOverdueSweep_noLateFee_balanceUnchanged() {
        Loan loan = buildLoan("TZ-2026-00000002", LoanStatus.OPEN, LocalDate.now().minusDays(1), BigDecimal.valueOf(10000));

        when(loanRepository.findOverdueLoans(any())).thenReturn(List.of(loan));
        when(feeCalculatorService.calculateLateFees(any(), any(), anyInt())).thenReturn(BigDecimal.ZERO);
        when(loanRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        overdueSweepJob.runOverdueSweep();

        assertThat(loan.getOutstandingBalance()).isEqualByComparingTo("10000.00");
    }

    private Loan buildLoan(String loanNumber, LoanStatus status, LocalDate dueDate, BigDecimal balance) {
        Loan loan = new Loan();
        loan.setId(UUID.randomUUID());
        loan.setProductId(UUID.randomUUID());
        loan.setCustomerId(UUID.randomUUID());
        loan.setLoanNumber(loanNumber);
        loan.setStatus(status);
        loan.setDueDate(dueDate);
        loan.setOutstandingBalance(balance);
        loan.setInstallments(new ArrayList<>());
        return loan;
    }
}
