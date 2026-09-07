package com.tezza.lending.product.internal.entity;

import com.tezza.lending.product.internal.entity.enums.CalculationType;
import com.tezza.lending.product.internal.entity.enums.FeeType;
import com.tezza.lending.shared.BaseAuditEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "PRODUCT_FEES")
public class ProductFee extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PRODUCT_ID", nullable = false)
    private LoanProduct product;

    @Enumerated(EnumType.STRING)
    @Column(name = "FEE_TYPE", nullable = false, length = 20)
    private FeeType feeType;

    @Enumerated(EnumType.STRING)
    @Column(name = "CALCULATION_TYPE", nullable = false, length = 10)
    private CalculationType calculationType;

    @Column(name = "AMOUNT", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "DAYS_AFTER_DUE", nullable = false)
    private int daysAfterDue = 0;

    @Column(name = "ACTIVE", nullable = false)
    private boolean active = true;

    public ProductFee() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public LoanProduct getProduct() { return product; }
    public void setProduct(LoanProduct product) { this.product = product; }
    public FeeType getFeeType() { return feeType; }
    public void setFeeType(FeeType feeType) { this.feeType = feeType; }
    public CalculationType getCalculationType() { return calculationType; }
    public void setCalculationType(CalculationType calculationType) { this.calculationType = calculationType; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public int getDaysAfterDue() { return daysAfterDue; }
    public void setDaysAfterDue(int daysAfterDue) { this.daysAfterDue = daysAfterDue; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
