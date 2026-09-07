package com.tezza.lending.product.api.dto;

import com.tezza.lending.product.internal.entity.enums.TenureType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class ProductRequest {
    @NotBlank(message = "Product name required") @Size(max = 100)
    private String name;
    private String description;
    @Positive(message = "Tenure value must be positive")
    private int tenureValue;
    @NotNull(message = "Tenure type required")
    private TenureType tenureType;
    @NotNull @DecimalMin("0.01")
    private BigDecimal minAmount;
    @NotNull @DecimalMin("0.01")
    private BigDecimal maxAmount;
    private int gracePeriodDays = 0;

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
}
