package com.tezza.lending.product.api.dto;

import com.tezza.lending.product.internal.entity.ProductFee;
import com.tezza.lending.product.internal.entity.enums.CalculationType;
import com.tezza.lending.product.internal.entity.enums.FeeType;

import java.math.BigDecimal;
import java.util.UUID;

public class FeeResponse {
    private UUID id;
    private FeeType feeType;
    private CalculationType calculationType;
    private BigDecimal amount;
    private int daysAfterDue;
    private boolean active;

    public static FeeResponse from(ProductFee f) {
        FeeResponse r = new FeeResponse();
        r.id = f.getId();
        r.feeType = f.getFeeType();
        r.calculationType = f.getCalculationType();
        r.amount = f.getAmount();
        r.daysAfterDue = f.getDaysAfterDue();
        r.active = f.isActive();
        return r;
    }

    public UUID getId() { return id; }
    public FeeType getFeeType() { return feeType; }
    public CalculationType getCalculationType() { return calculationType; }
    public BigDecimal getAmount() { return amount; }
    public int getDaysAfterDue() { return daysAfterDue; }
    public boolean isActive() { return active; }
}
