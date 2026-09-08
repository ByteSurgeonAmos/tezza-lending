package com.tezza.lending.loan.internal.service;

import com.tezza.lending.product.internal.entity.ProductFee;
import com.tezza.lending.product.internal.entity.enums.CalculationType;
import com.tezza.lending.product.internal.entity.enums.FeeType;
import com.tezza.lending.product.internal.repository.ProductFeeRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
public class FeeCalculatorService {

    private final ProductFeeRepository feeRepository;

    public FeeCalculatorService(ProductFeeRepository feeRepository) {
        this.feeRepository = feeRepository;
    }

    public BigDecimal calculateOriginationFees(UUID productId, BigDecimal principal) {
        List<ProductFee> fees = feeRepository.findByProductIdAndFeeType(productId, FeeType.SERVICE_FEE);
        return fees.stream()
                .filter(ProductFee::isActive)
                .map(fee -> applyFee(fee, principal))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal calculateDailyFee(UUID productId, BigDecimal balance) {
        List<ProductFee> fees = feeRepository.findByProductIdAndFeeType(productId, FeeType.DAILY_FEE);
        return fees.stream()
                .filter(ProductFee::isActive)
                .map(fee -> applyFee(fee, balance))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal calculateLateFees(UUID productId, BigDecimal balance, int daysOverdue) {
        List<ProductFee> fees = feeRepository.findByProductIdAndFeeType(productId, FeeType.LATE_FEE);
        return fees.stream()
                .filter(ProductFee::isActive)
                .filter(fee -> daysOverdue >= fee.getDaysAfterDue())
                .map(fee -> applyFee(fee, balance))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal applyFee(ProductFee fee, BigDecimal base) {
        if (fee.getCalculationType() == CalculationType.FIXED) {
            return fee.getAmount();
        }
        return base.multiply(fee.getAmount())
                   .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
}
