package com.tezza.lending.repayment;

import com.tezza.lending.loan.api.LoanService;
import com.tezza.lending.loan.api.RepaymentReceivedEvent;
import com.tezza.lending.loan.api.dto.LoanResponse;
import com.tezza.lending.loan.internal.entity.Loan;
import com.tezza.lending.loan.internal.entity.enums.LoanStatus;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RepaymentServiceTest {

    @Mock RepaymentRepository repaymentRepository;
    @Mock LoanService loanService;
    @Mock EntityManager entityManager;
    @Mock ApplicationEventPublisher eventPublisher;
    @InjectMocks RepaymentServiceImpl repaymentService;

    private LoanResponse openLoanResponse;
    private RepaymentRequest validRequest;

    @BeforeEach
    void setUp() {
        Loan loan = new Loan();
        loan.setId(UUID.randomUUID());
        loan.setCustomerId(UUID.randomUUID());
        loan.setLoanNumber("TZ-2026-00000001");
        loan.setStatus(LoanStatus.OPEN);
        loan.setOutstandingBalance(BigDecimal.valueOf(10000));
        loan.setInstallments(new ArrayList<>());
        openLoanResponse = LoanResponse.from(loan);

        validRequest = new RepaymentRequest();
        validRequest.setLoanId(openLoanResponse.getId());
        validRequest.setAmount(BigDecimal.valueOf(3000));
        validRequest.setReference("MPESA-12345");
        validRequest.setChannel(PaymentChannel.MPESA);

        Query mockQuery = mock(Query.class);
        lenient().when(mockQuery.setParameter(anyString(), any())).thenReturn(mockQuery);
        lenient().when(mockQuery.executeUpdate()).thenReturn(1);
        lenient().when(entityManager.createNativeQuery(anyString())).thenReturn(mockQuery);
    }

    @Test
    void processRepayment_partialPayment_callsStoredProcedures() {
        when(repaymentRepository.existsByReference("MPESA-12345")).thenReturn(false);
        when(loanService.getRepayableLoan(openLoanResponse.getId())).thenReturn(openLoanResponse);
        when(repaymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        repaymentService.processRepayment(validRequest);

        verify(entityManager).createNativeQuery(contains("proc_allocate_repayment"));
        verify(entityManager).createNativeQuery(contains("proc_close_loan_if_paid"));
    }

    @Test
    void processRepayment_closedLoan_throwsBusinessException() {
        when(repaymentRepository.existsByReference(any())).thenReturn(false);
        when(loanService.getRepayableLoan(openLoanResponse.getId()))
                .thenThrow(new BusinessException("Cannot process repayment for loan in status: CLOSED"));

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
        when(loanService.getRepayableLoan(openLoanResponse.getId())).thenReturn(openLoanResponse);
        when(repaymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        repaymentService.processRepayment(validRequest);

        verify(eventPublisher).publishEvent(any(RepaymentReceivedEvent.class));
    }
}
