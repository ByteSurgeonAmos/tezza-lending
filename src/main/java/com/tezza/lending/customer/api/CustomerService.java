package com.tezza.lending.customer.api;

import com.tezza.lending.customer.api.dto.CustomerRequest;
import com.tezza.lending.customer.api.dto.CustomerResponse;
import com.tezza.lending.customer.api.dto.LoanLimitRequest;
import com.tezza.lending.customer.api.dto.LoanLimitResponse;

import java.util.UUID;

public interface CustomerService {
    CustomerResponse createCustomer(CustomerRequest request);
    CustomerResponse getCustomer(UUID id);
    CustomerResponse updateCustomer(UUID id, CustomerRequest request);
    LoanLimitResponse getLoanLimit(UUID customerId);
    LoanLimitResponse updateLoanLimit(UUID customerId, LoanLimitRequest request);
}
