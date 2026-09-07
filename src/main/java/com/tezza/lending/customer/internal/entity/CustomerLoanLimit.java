package com.tezza.lending.customer.internal.entity;

import com.tezza.lending.shared.BaseAuditEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "CUSTOMER_LOAN_LIMITS")
public class CustomerLoanLimit extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID")
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CUSTOMER_ID", nullable = false, unique = true)
    private Customer customer;

    @Column(name = "MIN_LIMIT", nullable = false, precision = 19, scale = 2)
    private BigDecimal minLimit = BigDecimal.ZERO;

    @Column(name = "MAX_LIMIT", nullable = false, precision = 19, scale = 2)
    private BigDecimal maxLimit;

    @Column(name = "CURRENT_LIMIT", nullable = false, precision = 19, scale = 2)
    private BigDecimal currentLimit;

    @Column(name = "CREDIT_SCORE", nullable = false)
    private int creditScore = 0;

    @Column(name = "LAST_REVIEWED_AT")
    private LocalDateTime lastReviewedAt;

    public CustomerLoanLimit() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    public BigDecimal getMinLimit() { return minLimit; }
    public void setMinLimit(BigDecimal minLimit) { this.minLimit = minLimit; }
    public BigDecimal getMaxLimit() { return maxLimit; }
    public void setMaxLimit(BigDecimal maxLimit) { this.maxLimit = maxLimit; }
    public BigDecimal getCurrentLimit() { return currentLimit; }
    public void setCurrentLimit(BigDecimal currentLimit) { this.currentLimit = currentLimit; }
    public int getCreditScore() { return creditScore; }
    public void setCreditScore(int creditScore) { this.creditScore = creditScore; }
    public LocalDateTime getLastReviewedAt() { return lastReviewedAt; }
    public void setLastReviewedAt(LocalDateTime lastReviewedAt) { this.lastReviewedAt = lastReviewedAt; }
}
