package com.tezza.lending.loan.api.dto;

import com.tezza.lending.loan.internal.entity.Loan;
import com.tezza.lending.loan.internal.entity.enums.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class LoanResponse {
    private UUID id;
    private String loanNumber;
    private UUID customerId;
    private UUID productId;
    private BigDecimal principalAmount;
    private BigDecimal outstandingBalance;
    private LoanStatus status;
    private LoanType loanType;
    private BillingCycleType billingCycleType;
    private LocalDate dueDate;
    private LocalDate consolidatedDueDate;
    private LocalDateTime disbursedAt;
    private LocalDateTime closedAt;

    public static LoanResponse from(Loan l) {
        LoanResponse r = new LoanResponse();
        r.id = l.getId();
        r.loanNumber = l.getLoanNumber();
        r.customerId = l.getCustomerId();
        r.productId = l.getProductId();
        r.principalAmount = l.getPrincipalAmount();
        r.outstandingBalance = l.getOutstandingBalance();
        r.status = l.getStatus();
        r.loanType = l.getLoanType();
        r.billingCycleType = l.getBillingCycleType();
        r.dueDate = l.getDueDate();
        r.consolidatedDueDate = l.getConsolidatedDueDate();
        r.disbursedAt = l.getDisbursedAt();
        r.closedAt = l.getClosedAt();
        return r;
    }

    public UUID getId() { return id; }
    public String getLoanNumber() { return loanNumber; }
    public UUID getCustomerId() { return customerId; }
    public UUID getProductId() { return productId; }
    public BigDecimal getPrincipalAmount() { return principalAmount; }
    public BigDecimal getOutstandingBalance() { return outstandingBalance; }
    public LoanStatus getStatus() { return status; }
    public LoanType getLoanType() { return loanType; }
    public BillingCycleType getBillingCycleType() { return billingCycleType; }
    public LocalDate getDueDate() { return dueDate; }
    public LocalDate getConsolidatedDueDate() { return consolidatedDueDate; }
    public LocalDateTime getDisbursedAt() { return disbursedAt; }
    public LocalDateTime getClosedAt() { return closedAt; }
}
