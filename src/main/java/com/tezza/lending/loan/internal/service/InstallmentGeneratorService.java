package com.tezza.lending.loan.internal.service;

import com.tezza.lending.loan.internal.entity.Loan;
import com.tezza.lending.loan.internal.entity.LoanInstallment;
import com.tezza.lending.loan.internal.entity.enums.InstallmentStatus;
import com.tezza.lending.product.internal.entity.enums.TenureType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class InstallmentGeneratorService {

    public List<LoanInstallment> generate(
            Loan loan,
            int numInstallments,
            TenureType tenureType,
            int tenureValue,
            LocalDate originationDate,
            BigDecimal feePerInstallment) {

        BigDecimal principal = loan.getPrincipalAmount();
        BigDecimal installmentPrincipal = principal.divide(
                BigDecimal.valueOf(numInstallments), 2, RoundingMode.HALF_UP);
        BigDecimal lastPrincipal = principal.subtract(
                installmentPrincipal.multiply(BigDecimal.valueOf(numInstallments - 1)));

        List<LoanInstallment> installments = new ArrayList<>();

        for (int i = 1; i <= numInstallments; i++) {
            BigDecimal thisPrincipal = (i == numInstallments) ? lastPrincipal : installmentPrincipal;
            BigDecimal total = thisPrincipal.add(feePerInstallment);
            LocalDate dueDate = calculateDueDate(originationDate, tenureType, tenureValue, i);

            LoanInstallment inst = new LoanInstallment();
            inst.setLoan(loan);
            inst.setInstallmentNumber(i);
            inst.setPrincipalAmount(thisPrincipal);
            inst.setFeeAmount(feePerInstallment);
            inst.setTotalAmount(total);
            inst.setOutstandingAmount(total);
            inst.setDueDate(dueDate);
            inst.setStatus(InstallmentStatus.PENDING);
            installments.add(inst);
        }
        return installments;
    }

    private LocalDate calculateDueDate(LocalDate origin, TenureType type, int value, int installmentNumber) {
        if (type == TenureType.DAYS) {
            return origin.plusDays((long) value * installmentNumber);
        }
        return origin.plusMonths((long) value * installmentNumber);
    }
}
