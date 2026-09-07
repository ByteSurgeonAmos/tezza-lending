package com.tezza.lending.customer.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class LoanLimitRequest {
    @NotNull @DecimalMin("0.00") private BigDecimal minLimit;
    @NotNull @DecimalMin("0.01") private BigDecimal maxLimit;
    @NotNull @DecimalMin("0.00") private BigDecimal currentLimit;
    @Min(0) @Max(1000) private int creditScore;

    public BigDecimal getMinLimit() { return minLimit; }
    public void setMinLimit(BigDecimal minLimit) { this.minLimit = minLimit; }
    public BigDecimal getMaxLimit() { return maxLimit; }
    public void setMaxLimit(BigDecimal maxLimit) { this.maxLimit = maxLimit; }
    public BigDecimal getCurrentLimit() { return currentLimit; }
    public void setCurrentLimit(BigDecimal currentLimit) { this.currentLimit = currentLimit; }
    public int getCreditScore() { return creditScore; }
    public void setCreditScore(int creditScore) { this.creditScore = creditScore; }
}
