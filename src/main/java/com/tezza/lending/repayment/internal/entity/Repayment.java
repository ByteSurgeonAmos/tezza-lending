package com.tezza.lending.repayment.internal.entity;

import com.tezza.lending.repayment.internal.entity.enums.PaymentChannel;
import com.tezza.lending.shared.BaseAuditEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "REPAYMENTS")
public class Repayment extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID")
    private UUID id;

    @Column(name = "LOAN_ID", nullable = false)
    private UUID loanId;

    @Column(name = "AMOUNT", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "REFERENCE", nullable = false, unique = true, length = 100)
    private String reference;

    @Enumerated(EnumType.STRING)
    @Column(name = "CHANNEL", nullable = false, length = 10)
    private PaymentChannel channel;

    @Column(name = "PROCESSED_AT", nullable = false)
    private LocalDateTime processedAt;

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
