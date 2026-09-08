package com.tezza.lending.repayment;

import com.tezza.lending.loan.api.event.RepaymentReceivedEvent;
import com.tezza.lending.loan.internal.entity.Loan;
import com.tezza.lending.loan.internal.entity.LoanInstallment;
import com.tezza.lending.loan.internal.entity.enums.InstallmentStatus;
import com.tezza.lending.loan.internal.entity.enums.LoanStatus;
import com.tezza.lending.loan.internal.repository.LoanInstallmentRepository;
import com.tezza.lending.loan.internal.repository.LoanRepository;
import com.tezza.lending.repayment.api.dto.RepaymentRequest;
import com.tezza.lending.repayment.api.dto.RepaymentResponse;
import com.tezza.lending.repayment.internal.entity.enums.PaymentChannel;
import com.tezza.lending.repayment.internal.repository.RepaymentRepository;
import com.tezza.lending.repayment.internal.service.RepaymentServiceImpl;
import com.tezza.lending.shared.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RepaymentServiceTest {

    @Mock RepaymentRepository repaymentRepository;
    @Mock LoanRepository loanRepository;
    @Mock LoanInstallmentRepository installmentRepository;
    @Mock ApplicationEventPublisher eventPublisher;
    @InjectMocks RepaymentServiceImpl repaymentService;

    private Loan openLoan;
    private RepaymentRequest validRequest;

    @BeforeEach
    void setUp() {
        openLoan = new Loan();
        openLoan.setId(UUID.randomUUID());
        openLoan.setCustomerId(UUID.randomUUID());
        openLoan.setLoanNumber("TZ-2026-00000001");
        openLoan.setStatus(LoanStatus.OPEN);
        openLoan.setOutstandingBalance(BigDecimal.valueOf(10000));
        openLoan.setInstallments(new ArrayList<>());

        validRequest = new RepaymentRequest();
        validRequest.setLoanId(openLoan.getId());
        validRequest.setAmount(BigDecimal.valueOf(3000));
        validRequest.setReference("MPESA-12345");
        validRequest.setChannel(PaymentChannel.MPESA);
    }

    @Test
    void processRepayment_partialPayment_reducesBalance() {
        when(repaymentRepository.existsByReference("MPESA-12345")).thenReturn(false);
        when(loanRepository.findById(openLoan.getId())).thenReturn(Optional.of(openLoan));
        when(installmentRepository.findByLoanIdAndStatusOrderByDueDateAsc(any(), any())).thenReturn(List.of());
        when(repaymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(loanRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        repaymentService.processRepayment(validRequest);

        assertThat(openLoan.getOutstandingBalance()).isEqualByComparingTo("7000.00");
        assertThat(openLoan.getStatus()).isEqualTo(LoanStatus.OPEN);
    }

    @Test
    void processRepayment_fullPayment_closesLoan() {
        validRequest.setAmount(BigDecimal.valueOf(10000));
        when(repaymentRepository.existsByReference(any())).thenReturn(false);
        when(loanRepository.findById(openLoan.getId())).thenReturn(Optional.of(openLoan));
        when(installmentRepository.findByLoanIdAndStatusOrderByDueDateAsc(any(), any())).thenReturn(List.of());
        when(repaymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(loanRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        repaymentService.processRepayment(validRequest);

        assertThat(openLoan.getStatus()).isEqualTo(LoanStatus.CLOSED);
        assertThat(openLoan.getOutstandingBalance()).isEqualByComparingTo("0.00");
    }

    @Test
    void processRepayment_closedLoan_throwsBusinessException() {
        openLoan.setStatus(LoanStatus.CLOSED);
        when(repaymentRepository.existsByReference(any())).thenReturn(false);
        when(loanRepository.findById(openLoan.getId())).thenReturn(Optional.of(openLoan));

        assertThatThrownBy(() -> repaymentService.processRepayment(validRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("CLOSED");
    }

    @Test
    void processRepayment_duplicateReference_throwsBusinessException() {
        when(repaymentRepository.existsByReference("MPESA-12345")).thenReturn(true);

        assertThatThrownBy(() -> repaymentService.processRepayment(validRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Duplicate");
    }

    @Test
    void processRepayment_installmentAllocatedFifo() {
        LoanInstallment inst1 = new LoanInstallment();
        inst1.setId(UUID.randomUUID());
        inst1.setInstallmentNumber(1);
        inst1.setOutstandingAmount(BigDecimal.valueOf(3000));
        inst1.setDueDate(LocalDate.now().minusDays(30));
        inst1.setStatus(InstallmentStatus.PENDING);

        LoanInstallment inst2 = new LoanInstallment();
        inst2.setId(UUID.randomUUID());
        inst2.setInstallmentNumber(2);
        inst2.setOutstandingAmount(BigDecimal.valueOf(3000));
        inst2.setDueDate(LocalDate.now());
        inst2.setStatus(InstallmentStatus.PENDING);

        when(repaymentRepository.existsByReference(any())).thenReturn(false);
        when(loanRepository.findById(openLoan.getId())).thenReturn(Optional.of(openLoan));
        when(installmentRepository.findByLoanIdAndStatusOrderByDueDateAsc(openLoan.getId(), InstallmentStatus.PENDING))
                .thenReturn(List.of(inst1, inst2));
        when(repaymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(loanRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(installmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        validRequest.setAmount(BigDecimal.valueOf(3000));
        repaymentService.processRepayment(validRequest);

        assertThat(inst1.getStatus()).isEqualTo(InstallmentStatus.PAID);
        assertThat(inst2.getStatus()).isEqualTo(InstallmentStatus.PENDING);
    }

    @Test
    void processRepayment_publishesRepaymentReceivedEvent() {
        when(repaymentRepository.existsByReference(any())).thenReturn(false);
        when(loanRepository.findById(openLoan.getId())).thenReturn(Optional.of(openLoan));
        when(installmentRepository.findByLoanIdAndStatusOrderByDueDateAsc(any(), any())).thenReturn(List.of());
        when(repaymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(loanRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        repaymentService.processRepayment(validRequest);

        ArgumentCaptor<RepaymentReceivedEvent> captor = ArgumentCaptor.forClass(RepaymentReceivedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().amountPaid()).isEqualByComparingTo("3000.00");
    }
}
