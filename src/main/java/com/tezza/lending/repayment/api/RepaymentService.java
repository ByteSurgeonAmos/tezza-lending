package com.tezza.lending.repayment.api;

import com.tezza.lending.repayment.api.dto.RepaymentRequest;
import com.tezza.lending.repayment.api.dto.RepaymentResponse;

import java.util.List;
import java.util.UUID;

public interface RepaymentService {
    RepaymentResponse processRepayment(RepaymentRequest request);
    List<RepaymentResponse> getLoanRepayments(UUID loanId);
}
