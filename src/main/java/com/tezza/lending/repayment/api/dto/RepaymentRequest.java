package com.tezza.lending.repayment.api.dto;

import com.tezza.lending.repayment.internal.entity.enums.PaymentChannel;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.UUID;

public class RepaymentRequest {
    @NotNull private UUID loanId;
    @NotNull @DecimalMin("0.01") private BigDecimal amount;
    @NotBlank @Size(max = 100) private String reference;
    @NotNull private PaymentChannel channel;

    public UUID getLoanId() { return loanId; }
    public void setLoanId(UUID loanId) { this.loanId = loanId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public PaymentChannel getChannel() { return channel; }
    public void setChannel(PaymentChannel channel) { this.channel = channel; }
}
