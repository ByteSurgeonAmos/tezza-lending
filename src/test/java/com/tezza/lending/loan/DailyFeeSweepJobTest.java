package com.tezza.lending.loan;

import com.tezza.lending.loan.internal.entity.Loan;
import com.tezza.lending.loan.internal.entity.enums.LoanStatus;
import com.tezza.lending.loan.internal.repository.LoanRepository;
import com.tezza.lending.loan.internal.scheduler.DailyFeeSweepJob;
import com.tezza.lending.loan.internal.service.FeeCalculatorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DailyFeeSweepJobTest {

    @Mock LoanRepository loanRepository;
    @Mock FeeCalculatorService feeCalculatorService;
    @InjectMocks DailyFeeSweepJob dailyFeeSweepJob;

    @Test
    void runDailyFeeSweep_openLoanWithDailyFee_balanceIncreases() {
        Loan loan = new Loan();
        loan.setId(UUID.randomUUID());
        loan.setProductId(UUID.randomUUID());
        loan.setStatus(LoanStatus.OPEN);
        loan.setOutstandingBalance(BigDecimal.valueOf(50000));
        loan.setInstallments(new ArrayList<>());

        when(loanRepository.findLoansWithDailyFee()).thenReturn(List.of(loan));
        when(feeCalculatorService.calculateDailyFee(any(), any())).thenReturn(BigDecimal.valueOf(50));
        when(loanRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        dailyFeeSweepJob.runDailyFeeSweep();

        assertThat(loan.getOutstandingBalance()).isEqualByComparingTo("50050.00");
        verify(loanRepository).save(loan);
    }

    @Test
    void runDailyFeeSweep_zeroDailyFee_loanNotSaved() {
        Loan loan = new Loan();
        loan.setId(UUID.randomUUID());
        loan.setProductId(UUID.randomUUID());
        loan.setStatus(LoanStatus.OPEN);
        loan.setOutstandingBalance(BigDecimal.valueOf(50000));
        loan.setInstallments(new ArrayList<>());

        when(loanRepository.findLoansWithDailyFee()).thenReturn(List.of(loan));
        when(feeCalculatorService.calculateDailyFee(any(), any())).thenReturn(BigDecimal.ZERO);

        dailyFeeSweepJob.runDailyFeeSweep();

        verify(loanRepository, never()).save(any());
    }
}
