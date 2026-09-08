package com.tezza.lending.loan;

import com.tezza.lending.customer.internal.entity.Customer;
import com.tezza.lending.customer.internal.entity.CustomerLoanLimit;
import com.tezza.lending.customer.internal.repository.CustomerLoanLimitRepository;
import com.tezza.lending.customer.internal.repository.CustomerRepository;
import com.tezza.lending.loan.api.dto.LoanRequest;
import com.tezza.lending.loan.api.dto.LoanResponse;
import com.tezza.lending.loan.api.dto.LoanSummaryResponse;
import com.tezza.lending.loan.internal.entity.Loan;
import com.tezza.lending.loan.internal.entity.enums.*;
import com.tezza.lending.loan.internal.repository.LoanInstallmentRepository;
import com.tezza.lending.loan.internal.repository.LoanRepository;
import com.tezza.lending.loan.internal.service.FeeCalculatorService;
import com.tezza.lending.loan.internal.service.InstallmentGeneratorService;
import com.tezza.lending.loan.internal.service.LoanServiceImpl;
import com.tezza.lending.product.internal.entity.LoanProduct;
import com.tezza.lending.product.internal.entity.enums.TenureType;
import com.tezza.lending.product.internal.repository.LoanProductRepository;
import com.tezza.lending.shared.exception.BusinessException;
import com.tezza.lending.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

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
class LoanServiceTest {

    @Mock LoanRepository loanRepository;
    @Mock LoanInstallmentRepository installmentRepository;
    @Mock LoanProductRepository productRepository;
    @Mock CustomerRepository customerRepository;
    @Mock CustomerLoanLimitRepository loanLimitRepository;
    @Mock FeeCalculatorService feeCalculatorService;
    @Mock InstallmentGeneratorService installmentGeneratorService;
    @Mock ApplicationEventPublisher eventPublisher;
    @InjectMocks LoanServiceImpl loanService;

    private UUID customerId;
    private UUID productId;
    private LoanProduct product;
    private CustomerLoanLimit loanLimit;
    private LoanRequest validRequest;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        productId = UUID.randomUUID();

        product = new LoanProduct();
        product.setId(productId);
        product.setTenureValue(30);
        product.setTenureType(TenureType.DAYS);
        product.setMinAmount(BigDecimal.valueOf(1000));
        product.setMaxAmount(BigDecimal.valueOf(50000));
        product.setActive(true);
        product.setFees(new ArrayList<>());

        Customer customer = new Customer();
        customer.setId(customerId);

        loanLimit = new CustomerLoanLimit();
        loanLimit.setCustomer(customer);
        loanLimit.setCurrentLimit(BigDecimal.valueOf(50000));

        validRequest = new LoanRequest();
        validRequest.setCustomerId(customerId);
        validRequest.setProductId(productId);
        validRequest.setAmount(BigDecimal.valueOf(10000));
        validRequest.setLoanType(LoanType.LUMP_SUM);
        validRequest.setBillingCycleType(BillingCycleType.INDIVIDUAL);
    }

    @Test
    void disburseLoan_createsOpenLoan() {
        Customer customer = new Customer();
        customer.setId(customerId);
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(loanLimitRepository.findByCustomerId(customerId)).thenReturn(Optional.of(loanLimit));
        when(feeCalculatorService.calculateOriginationFees(any(), any())).thenReturn(BigDecimal.ZERO);
        when(loanRepository.save(any())).thenAnswer(inv -> {
            Loan l = inv.getArgument(0);
            if (l.getInstallments() == null) {
                l.setInstallments(new ArrayList<>());
            }
            return l;
        });

        LoanResponse result = loanService.disburseLoan(validRequest);

        assertThat(result.getStatus()).isEqualTo(LoanStatus.OPEN);
        assertThat(result.getPrincipalAmount()).isEqualByComparingTo("10000.00");
    }

    @Test
    void disburseLoan_amountExceedsCustomerLimit_throwsBusinessException() {
        validRequest.setAmount(BigDecimal.valueOf(40000));
        loanLimit.setCurrentLimit(BigDecimal.valueOf(30000));
        Customer customer = new Customer();
        customer.setId(customerId);
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(loanLimitRepository.findByCustomerId(customerId)).thenReturn(Optional.of(loanLimit));

        assertThatThrownBy(() -> loanService.disburseLoan(validRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("exceeds customer loan limit");
    }

    @Test
    void disburseLoan_inactiveProduct_throwsBusinessException() {
        product.setActive(false);
        Customer customer = new Customer();
        customer.setId(customerId);
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> loanService.disburseLoan(validRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not active");
    }

    @Test
    void disburseLoan_installmentType_requiresNumInstallments() {
        validRequest.setLoanType(LoanType.INSTALLMENT);
        validRequest.setNumInstallments(null);
        Customer customer = new Customer();
        customer.setId(customerId);
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(loanLimitRepository.findByCustomerId(customerId)).thenReturn(Optional.of(loanLimit));

        assertThatThrownBy(() -> loanService.disburseLoan(validRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("numInstallments required");
    }

    @Test
    void cancelLoan_openLoan_succeeds() {
        Loan loan = new Loan();
        loan.setId(UUID.randomUUID());
        loan.setStatus(LoanStatus.OPEN);
        loan.setInstallments(new ArrayList<>());
        when(loanRepository.findById(loan.getId())).thenReturn(Optional.of(loan));
        when(loanRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LoanResponse result = loanService.cancelLoan(loan.getId());

        assertThat(result.getStatus()).isEqualTo(LoanStatus.CANCELLED);
    }

    @Test
    void cancelLoan_closedLoan_throwsBusinessException() {
        Loan loan = new Loan();
        loan.setId(UUID.randomUUID());
        loan.setStatus(LoanStatus.CLOSED);
        loan.setInstallments(new ArrayList<>());
        when(loanRepository.findById(loan.getId())).thenReturn(Optional.of(loan));

        assertThatThrownBy(() -> loanService.cancelLoan(loan.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("CLOSED");
    }

    @Test
    void writeOffLoan_overdueLoan_succeeds() {
        Loan loan = new Loan();
        loan.setId(UUID.randomUUID());
        loan.setStatus(LoanStatus.OVERDUE);
        loan.setInstallments(new ArrayList<>());
        when(loanRepository.findById(loan.getId())).thenReturn(Optional.of(loan));
        when(loanRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LoanResponse result = loanService.writeOffLoan(loan.getId());

        assertThat(result.getStatus()).isEqualTo(LoanStatus.WRITTEN_OFF);
    }

    @Test
    void getLoanSummary_returnsCorrectTotals() {
        UUID loanId = UUID.randomUUID();
        Loan loan = new Loan();
        loan.setId(loanId);
        loan.setStatus(LoanStatus.OPEN);
        loan.setPrincipalAmount(BigDecimal.valueOf(10000));
        loan.setOutstandingBalance(BigDecimal.valueOf(7000));
        loan.setDueDate(LocalDate.now().plusDays(30));
        loan.setInstallments(new ArrayList<>());

        when(loanRepository.findById(loanId)).thenReturn(Optional.of(loan));
        when(installmentRepository.countByLoanIdAndStatus(loanId, InstallmentStatus.OVERDUE)).thenReturn(2);

        LoanSummaryResponse result = loanService.getLoanSummary(loanId);

        assertThat(result.getPrincipalAmount()).isEqualByComparingTo("10000");
        assertThat(result.getOutstandingBalance()).isEqualByComparingTo("7000");
        assertThat(result.getTotalPaid()).isEqualByComparingTo("3000");
        assertThat(result.getOverdueInstallmentCount()).isEqualTo(2);
    }

    @Test
    void getLoanSummary_notFound_throwsResourceNotFoundException() {
        UUID loanId = UUID.randomUUID();
        when(loanRepository.findById(loanId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loanService.getLoanSummary(loanId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getCustomerLoans_returnsPagedResults() {
        UUID customerId = UUID.randomUUID();
        var pageable = PageRequest.of(0, 20);
        Loan loan = new Loan();
        loan.setCustomerId(customerId);
        loan.setStatus(LoanStatus.OPEN);
        loan.setPrincipalAmount(BigDecimal.valueOf(5000));
        loan.setOutstandingBalance(BigDecimal.valueOf(5000));
        loan.setInstallments(new ArrayList<>());

        when(loanRepository.findByCustomerId(customerId, pageable))
                .thenReturn(new PageImpl<>(List.of(loan)));

        var result = loanService.getCustomerLoans(customerId, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getCustomerId()).isEqualTo(customerId);
    }

    @Test
    void writeOffLoan_openLoan_throwsBusinessException() {
        Loan loan = new Loan();
        loan.setId(UUID.randomUUID());
        loan.setStatus(LoanStatus.OPEN);
        loan.setInstallments(new ArrayList<>());
        when(loanRepository.findById(loan.getId())).thenReturn(Optional.of(loan));

        assertThatThrownBy(() -> loanService.writeOffLoan(loan.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("OVERDUE");
    }
}
