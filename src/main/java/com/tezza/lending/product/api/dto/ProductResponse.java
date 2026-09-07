package com.tezza.lending.product.api.dto;

import com.tezza.lending.product.internal.entity.LoanProduct;
import com.tezza.lending.product.internal.entity.enums.TenureType;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class ProductResponse {
    private UUID id;
    private String name;
    private String description;
    private int tenureValue;
    private TenureType tenureType;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private int gracePeriodDays;
    private boolean active;
    private List<FeeResponse> fees;

    public static ProductResponse from(LoanProduct p) {
        ProductResponse r = new ProductResponse();
        r.id = p.getId();
        r.name = p.getName();
        r.description = p.getDescription();
        r.tenureValue = p.getTenureValue();
        r.tenureType = p.getTenureType();
        r.minAmount = p.getMinAmount();
        r.maxAmount = p.getMaxAmount();
        r.gracePeriodDays = p.getGracePeriodDays();
        r.active = p.isActive();
        r.fees = p.getFees().stream().map(FeeResponse::from).toList();
        return r;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getTenureValue() { return tenureValue; }
    public TenureType getTenureType() { return tenureType; }
    public BigDecimal getMinAmount() { return minAmount; }
    public BigDecimal getMaxAmount() { return maxAmount; }
    public int getGracePeriodDays() { return gracePeriodDays; }
    public boolean isActive() { return active; }
    public List<FeeResponse> getFees() { return fees; }
}
