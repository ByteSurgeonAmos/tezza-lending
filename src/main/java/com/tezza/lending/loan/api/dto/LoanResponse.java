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
    public void setId(UUID id) { this.id = id; }
    public String getLoanNumber() { return loanNumber; }
    public void setLoanNumber(String loanNumber) { this.loanNumber = loanNumber; }
    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }
    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }
    public BigDecimal getPrincipalAmount() { return principalAmount; }
    public void setPrincipalAmount(BigDecimal principalAmount) { this.principalAmount = principalAmount; }
    public BigDecimal getOutstandingBalance() { return outstandingBalance; }
    public void setOutstandingBalance(BigDecimal outstandingBalance) { this.outstandingBalance = outstandingBalance; }
    public LoanStatus getStatus() { return status; }
    public void setStatus(LoanStatus status) { this.status = status; }
    public LoanType getLoanType() { return loanType; }
    public void setLoanType(LoanType loanType) { this.loanType = loanType; }
    public BillingCycleType getBillingCycleType() { return billingCycleType; }
    public void setBillingCycleType(BillingCycleType billingCycleType) { this.billingCycleType = billingCycleType; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public LocalDate getConsolidatedDueDate() { return consolidatedDueDate; }
    public void setConsolidatedDueDate(LocalDate consolidatedDueDate) { this.consolidatedDueDate = consolidatedDueDate; }
    public LocalDateTime getDisbursedAt() { return disbursedAt; }
    public void setDisbursedAt(LocalDateTime disbursedAt) { this.disbursedAt = disbursedAt; }
    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }
}
