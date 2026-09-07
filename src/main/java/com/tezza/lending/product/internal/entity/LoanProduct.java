package com.tezza.lending.product.internal.entity;

import com.tezza.lending.product.internal.entity.enums.TenureType;
import com.tezza.lending.shared.BaseAuditEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "LOAN_PRODUCTS")
public class LoanProduct extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID")
    private UUID id;

    @Column(name = "NAME", nullable = false, length = 100)
    private String name;

    @Column(name = "DESCRIPTION", columnDefinition = "TEXT")
    private String description;

    @Column(name = "TENURE_VALUE", nullable = false)
    private int tenureValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "TENURE_TYPE", nullable = false, length = 10)
    private TenureType tenureType;

    @Column(name = "MIN_AMOUNT", nullable = false, precision = 19, scale = 2)
    private BigDecimal minAmount;

    @Column(name = "MAX_AMOUNT", nullable = false, precision = 19, scale = 2)
    private BigDecimal maxAmount;

    @Column(name = "GRACE_PERIOD_DAYS", nullable = false)
    private int gracePeriodDays = 0;

    @Column(name = "ACTIVE", nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<ProductFee> fees = new ArrayList<>();

    public LoanProduct() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getTenureValue() { return tenureValue; }
    public void setTenureValue(int tenureValue) { this.tenureValue = tenureValue; }
    public TenureType getTenureType() { return tenureType; }
    public void setTenureType(TenureType tenureType) { this.tenureType = tenureType; }
    public BigDecimal getMinAmount() { return minAmount; }
    public void setMinAmount(BigDecimal minAmount) { this.minAmount = minAmount; }
    public BigDecimal getMaxAmount() { return maxAmount; }
    public void setMaxAmount(BigDecimal maxAmount) { this.maxAmount = maxAmount; }
    public int getGracePeriodDays() { return gracePeriodDays; }
    public void setGracePeriodDays(int gracePeriodDays) { this.gracePeriodDays = gracePeriodDays; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public List<ProductFee> getFees() { return fees; }
    public void setFees(List<ProductFee> fees) { this.fees = fees; }
}
