package com.tezza.lending.repayment.api.dto;

import com.tezza.lending.repayment.internal.entity.Repayment;
import com.tezza.lending.repayment.internal.entity.enums.PaymentChannel;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class RepaymentResponse {
    private UUID id;
    private UUID loanId;
    private BigDecimal amount;
    private String reference;
    private PaymentChannel channel;
    private LocalDateTime processedAt;

    public static RepaymentResponse from(Repayment r) {
        RepaymentResponse res = new RepaymentResponse();
        res.setId(r.getId());
        res.setLoanId(r.getLoanId());
        res.setAmount(r.getAmount());
        res.setReference(r.getReference());
        res.setChannel(r.getChannel());
        res.setProcessedAt(r.getProcessedAt());
        return res;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getLoanId() { return loanId; }
    public void setLoanId(UUID loanId) { this.loanId = loanId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public PaymentChannel getChannel() { return channel; }
    public void setChannel(PaymentChannel channel) { this.channel = channel; }
    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
}
