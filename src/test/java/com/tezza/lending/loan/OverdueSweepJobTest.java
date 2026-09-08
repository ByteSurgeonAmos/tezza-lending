package com.tezza.lending.loan;

import com.tezza.lending.loan.api.event.LoanOverdueEvent;
import com.tezza.lending.loan.internal.entity.Loan;
import com.tezza.lending.loan.internal.entity.enums.LoanStatus;
import com.tezza.lending.loan.internal.repository.LoanRepository;
import com.tezza.lending.loan.internal.scheduler.OverdueSweepJob;
import com.tezza.lending.loan.internal.service.FeeCalculatorService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OverdueSweepJobTest {

    @Mock LoanRepository loanRepository;
    @Mock FeeCalculatorService feeCalculatorService;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock EntityManager entityManager;
    @InjectMocks OverdueSweepJob overdueSweepJob;

    private Query mockQuery;

    @BeforeEach
    void setUp() {
        mockQuery = mock(Query.class);
        lenient().when(mockQuery.setParameter(anyString(), any())).thenReturn(mockQuery);
        lenient().when(mockQuery.executeUpdate()).thenReturn(1);
        lenient().when(entityManager.createNativeQuery(anyString())).thenReturn(mockQuery);
    }

    @Test
    void runOverdueSweep_overdueLoans_appliesLateFeeAndCallsSweepSP() {
        Loan loan = buildLoan("TZ-2026-00000001", LoanStatus.OPEN, LocalDate.now().minusDays(5), BigDecimal.valueOf(10000));
        when(loanRepository.findOverdueLoans(any())).thenReturn(List.of(loan));
        when(feeCalculatorService.calculateLateFees(any(), any(), anyInt())).thenReturn(BigDecimal.valueOf(500));
        when(loanRepository.findByStatus(eq(LoanStatus.OVERDUE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(loan)));

        overdueSweepJob.runOverdueSweep();

        // VERIFY late fee SP called
        verify(entityManager).createNativeQuery(contains("proc_apply_late_fee"));
        // VERIFY overdue sweep SP called
        verify(entityManager).createNativeQuery(contains("proc_run_overdue_sweep"));
    }

    @Test
    void runOverdueSweep_noOverdueLoans_noSpCalls() {
        when(loanRepository.findOverdueLoans(any())).thenReturn(List.of());
        when(loanRepository.findByStatus(eq(LoanStatus.OVERDUE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        overdueSweepJob.runOverdueSweep();

        // NO late fee SP since no loans
        verify(entityManager, never()).createNativeQuery(contains("proc_apply_late_fee"));
        // OVERDUE sweep SP still runs
        verify(entityManager).createNativeQuery(contains("proc_run_overdue_sweep"));
    }

    @Test
    void runOverdueSweep_publishesLoanOverdueEvent() {
        Loan loan = buildLoan("TZ-2026-00000001", LoanStatus.OPEN, LocalDate.now().minusDays(5), BigDecimal.valueOf(10000));
        when(loanRepository.findOverdueLoans(any())).thenReturn(List.of(loan));
        when(feeCalculatorService.calculateLateFees(any(), any(), anyInt())).thenReturn(BigDecimal.ZERO);
        when(loanRepository.findByStatus(eq(LoanStatus.OVERDUE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(loan)));

        overdueSweepJob.runOverdueSweep();

        verify(eventPublisher).publishEvent(any(LoanOverdueEvent.class));
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
