package com.tezza.lending.customer.internal.service;

import com.tezza.lending.customer.api.CustomerService;
import com.tezza.lending.customer.api.dto.CustomerRequest;
import com.tezza.lending.customer.api.dto.CustomerResponse;
import com.tezza.lending.customer.api.dto.LoanLimitRequest;
import com.tezza.lending.customer.api.dto.LoanLimitResponse;
import com.tezza.lending.customer.internal.entity.Customer;
import com.tezza.lending.customer.internal.entity.CustomerLoanLimit;
import com.tezza.lending.customer.internal.entity.enums.CustomerStatus;
import com.tezza.lending.customer.internal.repository.CustomerLoanLimitRepository;
import com.tezza.lending.customer.internal.repository.CustomerRepository;
import com.tezza.lending.shared.exception.BusinessException;
import com.tezza.lending.shared.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Transactional
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerLoanLimitRepository loanLimitRepository;

    public CustomerServiceImpl(CustomerRepository customerRepository,
                                CustomerLoanLimitRepository loanLimitRepository) {
        this.customerRepository = customerRepository;
        this.loanLimitRepository = loanLimitRepository;
    }

    @Override
    public CustomerResponse createCustomer(CustomerRequest request) {
        if (customerRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Email already registered: " + request.getEmail());
        }
        if (request.getNationalId() != null && customerRepository.existsByNationalId(request.getNationalId())) {
            throw new BusinessException("National ID already registered: " + request.getNationalId());
        }
        Customer customer = new Customer();
        customer.setFirstName(request.getFirstName());
        customer.setLastName(request.getLastName());
        customer.setEmail(request.getEmail());
        customer.setPhone(request.getPhone());
        customer.setNationalId(request.getNationalId());
        customer.setStatus(CustomerStatus.ACTIVE);
        return CustomerResponse.from(customerRepository.save(customer));
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerResponse getCustomer(UUID id) {
        return customerRepository.findById(id)
                .map(CustomerResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", id.toString()));
    }

    @Override
    public CustomerResponse updateCustomer(UUID id, CustomerRequest request) {
        Customer customer = findCustomer(id);
        customer.setFirstName(request.getFirstName());
        customer.setLastName(request.getLastName());
        customer.setPhone(request.getPhone());
        return CustomerResponse.from(customerRepository.save(customer));
    }

    @Override
    @Transactional(readOnly = true)
    public LoanLimitResponse getLoanLimit(UUID customerId) {
        findCustomer(customerId);
        return loanLimitRepository.findByCustomerId(customerId)
                .map(LoanLimitResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("LoanLimit for customer", customerId.toString()));
    }

    @Override
    public LoanLimitResponse updateLoanLimit(UUID customerId, LoanLimitRequest request) {
        Customer customer = findCustomer(customerId);
        if (customer.getStatus() == CustomerStatus.BLACKLISTED) {
            throw new BusinessException("Cannot update loan limit for blacklisted customer");
        }
        CustomerLoanLimit limit = loanLimitRepository.findByCustomerId(customerId)
                .orElseGet(() -> {
                    CustomerLoanLimit newLimit = new CustomerLoanLimit();
                    newLimit.setCustomer(customer);
                    return newLimit;
                });
        limit.setMinLimit(request.getMinLimit());
        limit.setMaxLimit(request.getMaxLimit());
        limit.setCurrentLimit(request.getCurrentLimit());
        limit.setCreditScore(request.getCreditScore());
        limit.setLastReviewedAt(LocalDateTime.now());
        return LoanLimitResponse.from(loanLimitRepository.save(limit));
    }

    private Customer findCustomer(UUID id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", id.toString()));
    }
}
