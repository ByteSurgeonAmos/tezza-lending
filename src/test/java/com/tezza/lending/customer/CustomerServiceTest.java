package com.tezza.lending.customer;

import com.tezza.lending.customer.api.dto.CustomerRequest;
import com.tezza.lending.customer.api.dto.LoanLimitRequest;
import com.tezza.lending.customer.internal.entity.Customer;
import com.tezza.lending.customer.internal.entity.CustomerLoanLimit;
import com.tezza.lending.customer.internal.entity.enums.CustomerStatus;
import com.tezza.lending.customer.internal.repository.CustomerLoanLimitRepository;
import com.tezza.lending.customer.internal.repository.CustomerRepository;
import com.tezza.lending.customer.internal.service.CustomerServiceImpl;
import com.tezza.lending.shared.exception.BusinessException;
import com.tezza.lending.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock CustomerRepository customerRepository;
    @Mock CustomerLoanLimitRepository loanLimitRepository;

    CustomerServiceImpl customerService;
    CustomerRequest validRequest;

    @BeforeEach
    void setUp() {
        customerService = new CustomerServiceImpl(customerRepository, loanLimitRepository);

        validRequest = new CustomerRequest();
        validRequest.setFirstName("Alice");
        validRequest.setLastName("Wanjiku");
        validRequest.setEmail("alice@example.com");
        validRequest.setNationalId("12345678");
    }

    @Test
    void createCustomer_success() {
        when(customerRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(customerRepository.existsByNationalId("12345678")).thenReturn(false);
        when(customerRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = customerService.createCustomer(validRequest);

        assertThat(result.getEmail()).isEqualTo("alice@example.com");
        assertThat(result.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
    }

    @Test
    void createCustomer_duplicateEmail_throwsBusinessException() {
        when(customerRepository.existsByEmail("alice@example.com")).thenReturn(true);

        assertThatThrownBy(() -> customerService.createCustomer(validRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already registered");
    }

    @Test
    void getCustomer_notFound_throwsResourceNotFoundException() {
        UUID id = UUID.randomUUID();
        when(customerRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.getCustomer(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateLoanLimit_blacklistedCustomer_throwsBusinessException() {
        UUID id = UUID.randomUUID();
        Customer blacklisted = new Customer();
        blacklisted.setId(id);
        blacklisted.setStatus(CustomerStatus.BLACKLISTED);
        when(customerRepository.findById(id)).thenReturn(Optional.of(blacklisted));

        LoanLimitRequest req = new LoanLimitRequest();
        req.setMinLimit(BigDecimal.ZERO);
        req.setMaxLimit(BigDecimal.valueOf(50000));
        req.setCurrentLimit(BigDecimal.valueOf(50000));

        assertThatThrownBy(() -> customerService.updateLoanLimit(id, req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("blacklisted");
    }

    @Test
    void updateLoanLimit_createsNewLimitIfAbsent() {
        UUID id = UUID.randomUUID();
        Customer customer = new Customer();
        customer.setId(id);
        customer.setStatus(CustomerStatus.ACTIVE);
        when(customerRepository.findById(id)).thenReturn(Optional.of(customer));
        when(loanLimitRepository.findByCustomerId(id)).thenReturn(Optional.empty());
        when(loanLimitRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LoanLimitRequest req = new LoanLimitRequest();
        req.setMinLimit(BigDecimal.valueOf(1000));
        req.setMaxLimit(BigDecimal.valueOf(50000));
        req.setCurrentLimit(BigDecimal.valueOf(50000));
        req.setCreditScore(700);

        var result = customerService.updateLoanLimit(id, req);

        assertThat(result.getMaxLimit()).isEqualByComparingTo(BigDecimal.valueOf(50000));
        assertThat(result.getCreditScore()).isEqualTo(700);
    }
}
