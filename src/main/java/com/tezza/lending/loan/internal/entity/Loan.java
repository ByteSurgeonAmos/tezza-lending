package com.tezza.lending.loan.internal.entity;

import com.tezza.lending.loan.internal.entity.enums.*;
import com.tezza.lending.shared.BaseAuditEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "LOANS")
public class Loan extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID")
    private UUID id;

    @Column(name = "LOAN_NUMBER", nullable = false, unique = true, length = 50)
    private String loanNumber;

    @Column(name = "CUSTOMER_ID", nullable = false)
    private UUID customerId;

    @Column(name = "PRODUCT_ID", nullable = false)
    private UUID productId;

    @Column(name = "PRINCIPAL_AMOUNT", nullable = false, precision = 19, scale = 2)
    private BigDecimal principalAmount;

    @Column(name = "DISBURSED_AMOUNT", precision = 19, scale = 2)
    private BigDecimal disbursedAmount;

    @Column(name = "OUTSTANDING_BALANCE", nullable = false, precision = 19, scale = 2)
    private BigDecimal outstandingBalance;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 15)
    private LoanStatus status = LoanStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(name = "LOAN_TYPE", nullable = false, length = 15)
    private LoanType loanType;

    @Enumerated(EnumType.STRING)
    @Column(name = "BILLING_CYCLE_TYPE", nullable = false, length = 15)
    private BillingCycleType billingCycleType = BillingCycleType.INDIVIDUAL;

    @Column(name = "CONSOLIDATED_DUE_DATE")
    private LocalDate consolidatedDueDate;

    @Column(name = "DUE_DATE", nullable = false)
    private LocalDate dueDate;

    @Column(name = "DISBURSED_AT")
    private LocalDateTime disbursedAt;

    @Column(name = "CLOSED_AT")
    private LocalDateTime closedAt;

    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<LoanInstallment> installments = new ArrayList<>();

    public Loan() {}

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
    public BigDecimal getDisbursedAmount() { return disbursedAmount; }
    public void setDisbursedAmount(BigDecimal disbursedAmount) { this.disbursedAmount = disbursedAmount; }
    public BigDecimal getOutstandingBalance() { return outstandingBalance; }
    public void setOutstandingBalance(BigDecimal outstandingBalance) { this.outstandingBalance = outstandingBalance; }
    public LoanStatus getStatus() { return status; }
    public void setStatus(LoanStatus status) { this.status = status; }
    public LoanType getLoanType() { return loanType; }
    public void setLoanType(LoanType loanType) { this.loanType = loanType; }
    public BillingCycleType getBillingCycleType() { return billingCycleType; }
    public void setBillingCycleType(BillingCycleType billingCycleType) { this.billingCycleType = billingCycleType; }
    public LocalDate getConsolidatedDueDate() { return consolidatedDueDate; }
    public void setConsolidatedDueDate(LocalDate consolidatedDueDate) { this.consolidatedDueDate = consolidatedDueDate; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public LocalDateTime getDisbursedAt() { return disbursedAt; }
    public void setDisbursedAt(LocalDateTime disbursedAt) { this.disbursedAt = disbursedAt; }
    public LocalDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(LocalDateTime closedAt) { this.closedAt = closedAt; }
    public List<LoanInstallment> getInstallments() { return installments; }
    public void setInstallments(List<LoanInstallment> installments) { this.installments = installments; }
}
