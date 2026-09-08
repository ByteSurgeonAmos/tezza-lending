package com.tezza.lending.loan;

import com.tezza.lending.loan.internal.entity.Loan;
import com.tezza.lending.loan.internal.entity.enums.LoanStatus;
import com.tezza.lending.loan.internal.repository.LoanRepository;
import com.tezza.lending.loan.internal.scheduler.DailyFeeSweepJob;
import com.tezza.lending.loan.internal.service.FeeCalculatorService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DailyFeeSweepJobTest {

    @Mock LoanRepository loanRepository;
    @Mock FeeCalculatorService feeCalculatorService;
    @Mock EntityManager entityManager;
    @InjectMocks DailyFeeSweepJob dailyFeeSweepJob;

    @BeforeEach
    void setUp() {
        Query mockQuery = mock(Query.class);
        lenient().when(mockQuery.setParameter(anyString(), any())).thenReturn(mockQuery);
        lenient().when(mockQuery.executeUpdate()).thenReturn(1);
        lenient().when(entityManager.createNativeQuery(anyString())).thenReturn(mockQuery);
    }

    @Test
    void runDailyFeeSweep_openLoanWithDailyFee_callsDailyFeeSP() {
        Loan loan = new Loan();
        loan.setId(UUID.randomUUID());
        loan.setProductId(UUID.randomUUID());
        loan.setStatus(LoanStatus.OPEN);
        loan.setOutstandingBalance(BigDecimal.valueOf(50000));
        loan.setInstallments(new ArrayList<>());

        when(loanRepository.findLoansWithDailyFee()).thenReturn(List.of(loan));
        when(feeCalculatorService.calculateDailyFee(any(), any())).thenReturn(BigDecimal.valueOf(50));

        dailyFeeSweepJob.runDailyFeeSweep();

        verify(entityManager).createNativeQuery(contains("proc_apply_daily_fee"));
    }

    @Test
    void runDailyFeeSweep_zeroDailyFee_noSpCall() {
        Loan loan = new Loan();
        loan.setId(UUID.randomUUID());
        loan.setProductId(UUID.randomUUID());
        loan.setStatus(LoanStatus.OPEN);
        loan.setOutstandingBalance(BigDecimal.valueOf(50000));
        loan.setInstallments(new ArrayList<>());

        when(loanRepository.findLoansWithDailyFee()).thenReturn(List.of(loan));
        when(feeCalculatorService.calculateDailyFee(any(), any())).thenReturn(BigDecimal.ZERO);

        dailyFeeSweepJob.runDailyFeeSweep();

        verify(entityManager, never()).createNativeQuery(contains("proc_apply_daily_fee"));
    }
}
