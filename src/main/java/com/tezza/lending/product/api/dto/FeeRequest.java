package com.tezza.lending.product.api.dto;

import com.tezza.lending.product.internal.entity.enums.CalculationType;
import com.tezza.lending.product.internal.entity.enums.FeeType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class FeeRequest {
    @NotNull private FeeType feeType;
    @NotNull private CalculationType calculationType;
    @NotNull @DecimalMin("0.00") private BigDecimal amount;
    @Min(0) private int daysAfterDue = 0;

    public FeeType getFeeType() { return feeType; }
    public void setFeeType(FeeType feeType) { this.feeType = feeType; }
    public CalculationType getCalculationType() { return calculationType; }
    public void setCalculationType(CalculationType calculationType) { this.calculationType = calculationType; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public int getDaysAfterDue() { return daysAfterDue; }
    public void setDaysAfterDue(int daysAfterDue) { this.daysAfterDue = daysAfterDue; }
}
