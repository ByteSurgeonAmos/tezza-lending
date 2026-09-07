package com.tezza.lending.customer.api.dto;

import com.tezza.lending.customer.internal.entity.CustomerLoanLimit;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class LoanLimitResponse {
    private UUID id;
    private UUID customerId;
    private BigDecimal minLimit;
    private BigDecimal maxLimit;
    private BigDecimal currentLimit;
    private int creditScore;
    private LocalDateTime lastReviewedAt;

    public static LoanLimitResponse from(CustomerLoanLimit l) {
        LoanLimitResponse r = new LoanLimitResponse();
        r.id = l.getId();
        r.customerId = l.getCustomer().getId();
        r.minLimit = l.getMinLimit();
        r.maxLimit = l.getMaxLimit();
        r.currentLimit = l.getCurrentLimit();
        r.creditScore = l.getCreditScore();
        r.lastReviewedAt = l.getLastReviewedAt();
        return r;
    }

    public UUID getId() { return id; }
    public UUID getCustomerId() { return customerId; }
    public BigDecimal getMinLimit() { return minLimit; }
    public BigDecimal getMaxLimit() { return maxLimit; }
    public BigDecimal getCurrentLimit() { return currentLimit; }
    public int getCreditScore() { return creditScore; }
    public LocalDateTime getLastReviewedAt() { return lastReviewedAt; }
}
