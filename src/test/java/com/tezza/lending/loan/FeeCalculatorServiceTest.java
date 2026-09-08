package com.tezza.lending.loan;

import com.tezza.lending.loan.internal.service.FeeCalculatorService;
import com.tezza.lending.product.internal.entity.ProductFee;
import com.tezza.lending.product.internal.entity.enums.CalculationType;
import com.tezza.lending.product.internal.entity.enums.FeeType;
import com.tezza.lending.product.internal.repository.ProductFeeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeeCalculatorServiceTest {

    @Mock ProductFeeRepository feeRepository;
    @InjectMocks FeeCalculatorService feeCalculatorService;

    private final UUID productId = UUID.randomUUID();

    @Test
    void calculateOriginationFees_fixedFee_returnsExactAmount() {
        ProductFee fee = buildFee(FeeType.SERVICE_FEE, CalculationType.FIXED, "500.00", 0);
        when(feeRepository.findByProductIdAndFeeType(productId, FeeType.SERVICE_FEE)).thenReturn(List.of(fee));

        BigDecimal result = feeCalculatorService.calculateOriginationFees(productId, BigDecimal.valueOf(10000));

        assertThat(result).isEqualByComparingTo("500.00");
    }

    @Test
    void calculateOriginationFees_percentageFee_returnsCorrectAmount() {
        ProductFee fee = buildFee(FeeType.SERVICE_FEE, CalculationType.PERCENTAGE, "5.00", 0);
        when(feeRepository.findByProductIdAndFeeType(productId, FeeType.SERVICE_FEE)).thenReturn(List.of(fee));

        BigDecimal result = feeCalculatorService.calculateOriginationFees(productId, BigDecimal.valueOf(10000));

        assertThat(result).isEqualByComparingTo("500.00");
    }

    @Test
    void calculateDailyFee_percentageFee_returnsCorrectDailyAmount() {
        ProductFee fee = buildFee(FeeType.DAILY_FEE, CalculationType.PERCENTAGE, "0.10", 0);
        when(feeRepository.findByProductIdAndFeeType(productId, FeeType.DAILY_FEE)).thenReturn(List.of(fee));

        BigDecimal result = feeCalculatorService.calculateDailyFee(productId, BigDecimal.valueOf(50000));

        assertThat(result).isEqualByComparingTo("50.00");
    }

    @Test
    void calculateLateFees_beforeTriggerDays_returnsZero() {
        ProductFee fee = buildFee(FeeType.LATE_FEE, CalculationType.FIXED, "500.00", 3);
        when(feeRepository.findByProductIdAndFeeType(productId, FeeType.LATE_FEE)).thenReturn(List.of(fee));

        BigDecimal result = feeCalculatorService.calculateLateFees(productId, BigDecimal.valueOf(10000), 2);

        assertThat(result).isEqualByComparingTo("0.00");
    }

    @Test
    void calculateLateFees_onTriggerDay_appliesFee() {
        ProductFee fee = buildFee(FeeType.LATE_FEE, CalculationType.FIXED, "500.00", 3);
        when(feeRepository.findByProductIdAndFeeType(productId, FeeType.LATE_FEE)).thenReturn(List.of(fee));

        BigDecimal result = feeCalculatorService.calculateLateFees(productId, BigDecimal.valueOf(10000), 3);

        assertThat(result).isEqualByComparingTo("500.00");
    }

    @Test
    void calculateLateFees_afterTriggerDay_appliesFee() {
        ProductFee fee = buildFee(FeeType.LATE_FEE, CalculationType.PERCENTAGE, "2.00", 5);
        when(feeRepository.findByProductIdAndFeeType(productId, FeeType.LATE_FEE)).thenReturn(List.of(fee));

        BigDecimal result = feeCalculatorService.calculateLateFees(productId, BigDecimal.valueOf(20000), 10);

        assertThat(result).isEqualByComparingTo("400.00");
    }

    private ProductFee buildFee(FeeType type, CalculationType calc, String amount, int daysAfterDue) {
        ProductFee fee = new ProductFee();
        fee.setFeeType(type);
        fee.setCalculationType(calc);
        fee.setAmount(new BigDecimal(amount));
        fee.setDaysAfterDue(daysAfterDue);
        fee.setActive(true);
        return fee;
    }
}
