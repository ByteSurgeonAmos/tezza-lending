package com.tezza.lending.loan.api.dto;

import com.tezza.lending.loan.internal.entity.enums.BillingCycleType;
import com.tezza.lending.loan.internal.entity.enums.LoanType;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class LoanRequest {
    @NotNull private UUID customerId;
    @NotNull private UUID productId;
    @NotNull @DecimalMin("0.01") private BigDecimal amount;
    @NotNull private LoanType loanType;
    @NotNull private BillingCycleType billingCycleType;
    @Min(1) private Integer numInstallments;
    private LocalDate consolidatedDueDate;

    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }
    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public LoanType getLoanType() { return loanType; }
    public void setLoanType(LoanType loanType) { this.loanType = loanType; }
    public BillingCycleType getBillingCycleType() { return billingCycleType; }
    public void setBillingCycleType(BillingCycleType billingCycleType) { this.billingCycleType = billingCycleType; }
    public Integer getNumInstallments() { return numInstallments; }
    public void setNumInstallments(Integer numInstallments) { this.numInstallments = numInstallments; }
    public LocalDate getConsolidatedDueDate() { return consolidatedDueDate; }
    public void setConsolidatedDueDate(LocalDate consolidatedDueDate) { this.consolidatedDueDate = consolidatedDueDate; }
}
