package com.tezza.lending.loan.api;

import com.tezza.lending.loan.api.dto.*;
import com.tezza.lending.loan.internal.entity.enums.LoanStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface LoanService {
    LoanResponse disburseLoan(LoanRequest request);
    LoanResponse getLoan(UUID id);
    Page<LoanResponse> listLoans(LoanStatus status, Pageable pageable);
    Page<LoanResponse> getCustomerLoans(UUID customerId, Pageable pageable);
    List<InstallmentResponse> getInstallments(UUID loanId);
    LoanResponse cancelLoan(UUID id);
    LoanResponse writeOffLoan(UUID id);
}
