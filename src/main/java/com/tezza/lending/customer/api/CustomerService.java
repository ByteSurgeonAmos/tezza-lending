package com.tezza.lending.customer.api;

import com.tezza.lending.customer.api.dto.CustomerRequest;
import com.tezza.lending.customer.api.dto.CustomerResponse;
import com.tezza.lending.customer.api.dto.CustomerStatusRequest;
import com.tezza.lending.customer.api.dto.LoanLimitRequest;
import com.tezza.lending.customer.api.dto.LoanLimitResponse;
import com.tezza.lending.customer.internal.entity.enums.CustomerStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface CustomerService {
    CustomerResponse createCustomer(CustomerRequest request);
    Page<CustomerResponse> listCustomers(CustomerStatus status, Pageable pageable);
    CustomerResponse getCustomer(UUID id);
    CustomerResponse updateCustomer(UUID id, CustomerRequest request);
    CustomerResponse updateStatus(UUID id, CustomerStatusRequest request);
    LoanLimitResponse getLoanLimit(UUID customerId);
    LoanLimitResponse updateLoanLimit(UUID customerId, LoanLimitRequest request);
}
