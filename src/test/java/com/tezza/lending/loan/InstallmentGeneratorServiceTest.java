package com.tezza.lending.loan;

import com.tezza.lending.loan.internal.entity.Loan;
import com.tezza.lending.loan.internal.entity.LoanInstallment;
import com.tezza.lending.loan.internal.entity.enums.InstallmentStatus;
import com.tezza.lending.loan.internal.service.InstallmentGeneratorService;
import com.tezza.lending.product.internal.entity.enums.TenureType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InstallmentGeneratorServiceTest {

    private final InstallmentGeneratorService service = new InstallmentGeneratorService();
    private final LocalDate originationDate = LocalDate.of(2026, 9, 7);

    private Loan buildLoan(BigDecimal principal) {
        Loan loan = new Loan();
        loan.setId(UUID.randomUUID());
        loan.setPrincipalAmount(principal);
        return loan;
    }

    @Test
    void generate_3Installments_principalSplitEqually() {
        Loan loan = buildLoan(BigDecimal.valueOf(30000));
        List<LoanInstallment> installments = service.generate(
                loan, 3, TenureType.DAYS, 30, originationDate, BigDecimal.ZERO);

        assertThat(installments).hasSize(3);
        assertThat(installments.get(0).getPrincipalAmount()).isEqualByComparingTo("10000.00");
        assertThat(installments.get(1).getPrincipalAmount()).isEqualByComparingTo("10000.00");
        assertThat(installments.get(2).getPrincipalAmount()).isEqualByComparingTo("10000.00");
    }

    @Test
    void generate_dueDatesSpaced30DaysApart() {
        Loan loan = buildLoan(BigDecimal.valueOf(30000));
        List<LoanInstallment> installments = service.generate(
                loan, 3, TenureType.DAYS, 30, originationDate, BigDecimal.ZERO);

        assertThat(installments.get(0).getDueDate()).isEqualTo(originationDate.plusDays(30));
        assertThat(installments.get(1).getDueDate()).isEqualTo(originationDate.plusDays(60));
        assertThat(installments.get(2).getDueDate()).isEqualTo(originationDate.plusDays(90));
    }

    @Test
    void generate_monthlyTenure_dueDatesSpacedByMonths() {
        Loan loan = buildLoan(BigDecimal.valueOf(120000));
        List<LoanInstallment> installments = service.generate(
                loan, 12, TenureType.MONTHS, 1, originationDate, BigDecimal.ZERO);

        assertThat(installments.get(0).getDueDate()).isEqualTo(originationDate.plusMonths(1));
        assertThat(installments.get(11).getDueDate()).isEqualTo(originationDate.plusMonths(12));
    }

    @Test
    void generate_feeIncludedInTotalAmount() {
        Loan loan = buildLoan(BigDecimal.valueOf(30000));
        BigDecimal feePerInstallment = BigDecimal.valueOf(500);
        List<LoanInstallment> installments = service.generate(
                loan, 3, TenureType.DAYS, 30, originationDate, feePerInstallment);

        assertThat(installments.get(0).getTotalAmount()).isEqualByComparingTo("10500.00");
        assertThat(installments.get(0).getFeeAmount()).isEqualByComparingTo("500.00");
    }

    @Test
    void generate_installmentNumbersSequential() {
        Loan loan = buildLoan(BigDecimal.valueOf(10000));
        List<LoanInstallment> installments = service.generate(
                loan, 4, TenureType.DAYS, 30, originationDate, BigDecimal.ZERO);

        for (int i = 0; i < 4; i++) {
            assertThat(installments.get(i).getInstallmentNumber()).isEqualTo(i + 1);
        }
    }

    @Test
    void generate_allInstallmentsStartPending() {
        Loan loan = buildLoan(BigDecimal.valueOf(10000));
        List<LoanInstallment> installments = service.generate(
                loan, 2, TenureType.DAYS, 30, originationDate, BigDecimal.ZERO);

        assertThat(installments).allMatch(i -> i.getStatus() == InstallmentStatus.PENDING);
    }

    @Test
    void generate_oddPrincipal_lastInstallmentAbsorbsRemainder() {
        Loan loan = buildLoan(BigDecimal.valueOf(10001));
        List<LoanInstallment> installments = service.generate(
                loan, 3, TenureType.DAYS, 30, originationDate, BigDecimal.ZERO);

        BigDecimal sum = installments.stream()
                .map(LoanInstallment::getPrincipalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sum).isEqualByComparingTo("10001.00");
    }
}
