package com.tezza.lending.repayment;

import com.tezza.lending.loan.api.event.RepaymentReceivedEvent;
import com.tezza.lending.loan.internal.entity.Loan;
import com.tezza.lending.loan.internal.entity.enums.LoanStatus;
import com.tezza.lending.loan.internal.repository.LoanRepository;
import com.tezza.lending.repayment.api.dto.RepaymentRequest;
import com.tezza.lending.repayment.internal.entity.enums.PaymentChannel;
import com.tezza.lending.repayment.internal.repository.RepaymentRepository;
import com.tezza.lending.repayment.internal.service.RepaymentServiceImpl;
import com.tezza.lending.shared.exception.BusinessException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RepaymentServiceTest {

    @Mock RepaymentRepository repaymentRepository;
    @Mock LoanRepository loanRepository;
    @Mock EntityManager entityManager;
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

        // DEFAULT mock for entityManager queries (lenient — only some tests hit SP paths)
        Query mockQuery = mock(Query.class);
        lenient().when(mockQuery.setParameter(anyString(), any())).thenReturn(mockQuery);
        lenient().when(mockQuery.executeUpdate()).thenReturn(1);
        lenient().when(entityManager.createNativeQuery(anyString())).thenReturn(mockQuery);
    }

    @Test
    void processRepayment_partialPayment_callsStoredProcedures() {
        when(repaymentRepository.existsByReference("MPESA-12345")).thenReturn(false);
        when(loanRepository.findById(openLoan.getId())).thenReturn(Optional.of(openLoan));
        when(repaymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        repaymentService.processRepayment(validRequest);

        // VERIFY FIFO allocation SP was called
        verify(entityManager).createNativeQuery(contains("proc_allocate_repayment"));
        // VERIFY close-if-paid SP was called
        verify(entityManager).createNativeQuery(contains("proc_close_loan_if_paid"));
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
    void processRepayment_publishesRepaymentReceivedEvent() {
        when(repaymentRepository.existsByReference(any())).thenReturn(false);
        when(loanRepository.findById(openLoan.getId())).thenReturn(Optional.of(openLoan));
        when(repaymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        repaymentService.processRepayment(validRequest);

        verify(eventPublisher).publishEvent(any(RepaymentReceivedEvent.class));
    }
}
