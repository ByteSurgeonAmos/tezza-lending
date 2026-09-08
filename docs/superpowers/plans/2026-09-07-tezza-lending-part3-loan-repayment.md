# Tezza Lending — Part 3: Loan & Repayment Modules

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Full loan lifecycle — disburse, installment generation, fee calculation, state transitions, sweep jobs, repayment processing with balance allocation.

**Depends on:** Part 2 complete (auth, product, customer modules exist)

---

## Task 7: Loan Module — Core Entities & Disbursement

**Files:**
- Create: `src/main/java/com/tezza/lending/loan/internal/entity/Loan.java`
- Create: `src/main/java/com/tezza/lending/loan/internal/entity/LoanInstallment.java`
- Create: `src/main/java/com/tezza/lending/loan/internal/entity/enums/LoanStatus.java`
- Create: `src/main/java/com/tezza/lending/loan/internal/entity/enums/LoanType.java`
- Create: `src/main/java/com/tezza/lending/loan/internal/entity/enums/BillingCycleType.java`
- Create: `src/main/java/com/tezza/lending/loan/internal/entity/enums/InstallmentStatus.java`
- Create: `src/main/java/com/tezza/lending/loan/internal/repository/LoanRepository.java`
- Create: `src/main/java/com/tezza/lending/loan/internal/repository/LoanInstallmentRepository.java`
- Create: `src/main/java/com/tezza/lending/loan/internal/service/FeeCalculatorService.java`
- Create: `src/main/java/com/tezza/lending/loan/internal/service/InstallmentGeneratorService.java`
- Create: `src/main/java/com/tezza/lending/loan/internal/service/LoanServiceImpl.java`
- Create: `src/main/java/com/tezza/lending/loan/api/LoanService.java`
- Create: `src/main/java/com/tezza/lending/loan/api/event/LoanCreatedEvent.java`
- Create: `src/main/java/com/tezza/lending/loan/api/event/LoanOverdueEvent.java`
- Create: `src/main/java/com/tezza/lending/loan/api/event/DueDateReminderEvent.java`
- Create: `src/main/java/com/tezza/lending/loan/api/event/RepaymentReceivedEvent.java`
- Create: `src/main/java/com/tezza/lending/loan/api/dto/LoanRequest.java`
- Create: `src/main/java/com/tezza/lending/loan/api/dto/LoanResponse.java`
- Create: `src/main/java/com/tezza/lending/loan/api/dto/InstallmentResponse.java`
- Create: `src/main/java/com/tezza/lending/loan/web/LoanController.java`
- Test: `src/test/java/com/tezza/lending/loan/FeeCalculatorServiceTest.java`
- Test: `src/test/java/com/tezza/lending/loan/InstallmentGeneratorServiceTest.java`
- Test: `src/test/java/com/tezza/lending/loan/LoanServiceTest.java`

- [ ] **Step 1: Create enums**

```java
// LoanStatus.java
package com.tezza.lending.loan.internal.entity.enums;
public enum LoanStatus { OPEN, CLOSED, CANCELLED, OVERDUE, WRITTEN_OFF }

// LoanType.java
package com.tezza.lending.loan.internal.entity.enums;
public enum LoanType { LUMP_SUM, INSTALLMENT }

// BillingCycleType.java
package com.tezza.lending.loan.internal.entity.enums;
public enum BillingCycleType { INDIVIDUAL, CONSOLIDATED }

// InstallmentStatus.java
package com.tezza.lending.loan.internal.entity.enums;
public enum InstallmentStatus { PENDING, PAID, OVERDUE }
```

- [ ] **Step 2: Create Loan entity**

```java
package com.tezza.lending.loan.internal.entity;

import com.tezza.lending.loan.internal.entity.enums.*;
import com.tezza.lending.shared.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "LOANS")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Loan extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID")
    private UUID id;

    @Column(name = "LOAN_NUMBER", nullable = false, unique = true, length = 50)
    private String loanNumber;

    @Column(name = "CUSTOMER_ID", nullable = false)
    private UUID customerId;

    @Column(name = "PRODUCT_ID", nullable = false)
    private UUID productId;

    @Column(name = "PRINCIPAL_AMOUNT", nullable = false, precision = 19, scale = 2)
    private BigDecimal principalAmount;

    @Column(name = "DISBURSED_AMOUNT", precision = 19, scale = 2)
    private BigDecimal disbursedAmount;

    @Column(name = "OUTSTANDING_BALANCE", nullable = false, precision = 19, scale = 2)
    private BigDecimal outstandingBalance;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 15)
    @Builder.Default
    private LoanStatus status = LoanStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(name = "LOAN_TYPE", nullable = false, length = 15)
    private LoanType loanType;

    @Enumerated(EnumType.STRING)
    @Column(name = "BILLING_CYCLE_TYPE", nullable = false, length = 15)
    @Builder.Default
    private BillingCycleType billingCycleType = BillingCycleType.INDIVIDUAL;

    @Column(name = "CONSOLIDATED_DUE_DATE")
    private LocalDate consolidatedDueDate;

    @Column(name = "DUE_DATE", nullable = false)
    private LocalDate dueDate;

    @Column(name = "DISBURSED_AT")
    private LocalDateTime disbursedAt;

    @Column(name = "CLOSED_AT")
    private LocalDateTime closedAt;

    @OneToMany(mappedBy = "loan", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<LoanInstallment> installments = new ArrayList<>();
}
```

- [ ] **Step 3: Create LoanInstallment entity**

```java
package com.tezza.lending.loan.internal.entity;

import com.tezza.lending.loan.internal.entity.enums.InstallmentStatus;
import com.tezza.lending.shared.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "LOAN_INSTALLMENTS")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LoanInstallment extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "LOAN_ID", nullable = false)
    private Loan loan;

    @Column(name = "INSTALLMENT_NUMBER", nullable = false)
    private int installmentNumber;

    @Column(name = "PRINCIPAL_AMOUNT", nullable = false, precision = 19, scale = 2)
    private BigDecimal principalAmount;

    @Column(name = "FEE_AMOUNT", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal feeAmount = BigDecimal.ZERO;

    @Column(name = "TOTAL_AMOUNT", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "OUTSTANDING_AMOUNT", nullable = false, precision = 19, scale = 2)
    private BigDecimal outstandingAmount;

    @Column(name = "DUE_DATE", nullable = false)
    private LocalDate dueDate;

    @Column(name = "PAID_AT")
    private LocalDateTime paidAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "STATUS", nullable = false, length = 10)
    @Builder.Default
    private InstallmentStatus status = InstallmentStatus.PENDING;
}
```

- [ ] **Step 4: Create repositories**

```java
// LoanRepository.java
package com.tezza.lending.loan.internal.repository;

import com.tezza.lending.loan.internal.entity.Loan;
import com.tezza.lending.loan.internal.entity.enums.LoanStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface LoanRepository extends JpaRepository<Loan, UUID> {
    Page<Loan> findByCustomerId(UUID customerId, Pageable pageable);
    Page<Loan> findByStatus(LoanStatus status, Pageable pageable);

    @Query("SELECT l FROM Loan l WHERE l.dueDate < :today AND l.status = 'OPEN'")
    List<Loan> findOverdueLoans(@Param("today") LocalDate today);

    @Query("""
        SELECT DISTINCT l FROM Loan l JOIN l.installments i
        JOIN com.tezza.lending.product.internal.entity.ProductFee pf
          ON pf.product.id = l.productId AND pf.feeType = 'DAILY_FEE' AND pf.active = true
        WHERE l.status IN ('OPEN', 'OVERDUE')
    """)
    List<Loan> findLoansWithDailyFee();

    @Query("SELECT l FROM Loan l WHERE l.dueDate = :targetDate AND l.status = 'OPEN'")
    List<Loan> findLoansDueOn(@Param("targetDate") LocalDate targetDate);
}
```

```java
// LoanInstallmentRepository.java
package com.tezza.lending.loan.internal.repository;

import com.tezza.lending.loan.internal.entity.LoanInstallment;
import com.tezza.lending.loan.internal.entity.enums.InstallmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface LoanInstallmentRepository extends JpaRepository<LoanInstallment, UUID> {
    List<LoanInstallment> findByLoanIdOrderByInstallmentNumberAsc(UUID loanId);
    List<LoanInstallment> findByLoanIdAndStatusOrderByDueDateAsc(UUID loanId, InstallmentStatus status);
    List<LoanInstallment> findByDueDateBeforeAndStatus(LocalDate date, InstallmentStatus status);
}
```

- [ ] **Step 5: Create Spring Application Events**

```java
// LoanCreatedEvent.java
package com.tezza.lending.loan.api.event;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record LoanCreatedEvent(
    UUID loanId,
    UUID customerId,
    String loanNumber,
    BigDecimal amount,
    LocalDate dueDate
) {}
```

```java
// LoanOverdueEvent.java
package com.tezza.lending.loan.api.event;

import java.math.BigDecimal;
import java.util.UUID;

public record LoanOverdueEvent(
    UUID loanId,
    UUID customerId,
    String loanNumber,
    BigDecimal outstandingBalance
) {}
```

```java
// DueDateReminderEvent.java
package com.tezza.lending.loan.api.event;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record DueDateReminderEvent(
    UUID loanId,
    UUID customerId,
    String loanNumber,
    BigDecimal outstandingBalance,
    LocalDate dueDate
) {}
```

```java
// RepaymentReceivedEvent.java
package com.tezza.lending.loan.api.event;

import java.math.BigDecimal;
import java.util.UUID;

public record RepaymentReceivedEvent(
    UUID loanId,
    UUID customerId,
    String loanNumber,
    BigDecimal amountPaid,
    BigDecimal remainingBalance
) {}
```

- [ ] **Step 6: Create FeeCalculatorService**

```java
package com.tezza.lending.loan.internal.service;

import com.tezza.lending.product.internal.entity.ProductFee;
import com.tezza.lending.product.internal.entity.enums.CalculationType;
import com.tezza.lending.product.internal.entity.enums.FeeType;
import com.tezza.lending.product.internal.repository.ProductFeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FeeCalculatorService {

    private final ProductFeeRepository feeRepository;

    /** Calculates total origination/service fee for a loan at disbursement. */
    public BigDecimal calculateOriginationFees(UUID productId, BigDecimal principal) {
        List<ProductFee> fees = feeRepository.findByProductIdAndFeeType(productId, FeeType.SERVICE_FEE);
        return fees.stream()
                .filter(ProductFee::isActive)
                .map(fee -> applyFee(fee, principal))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Calculates daily accrual fee for a loan. */
    public BigDecimal calculateDailyFee(UUID productId, BigDecimal balance) {
        List<ProductFee> fees = feeRepository.findByProductIdAndFeeType(productId, FeeType.DAILY_FEE);
        return fees.stream()
                .filter(ProductFee::isActive)
                .map(fee -> applyFee(fee, balance))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Calculates late fee when daysOverdue >= fee.daysAfterDue. */
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
        // PERCENTAGE: base * rate / 100
        return base.multiply(fee.getAmount())
                   .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
}
```

- [ ] **Step 7: Write FeeCalculatorService tests first**

Create `src/test/java/com/tezza/lending/loan/FeeCalculatorServiceTest.java`:

```java
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

        // 5% of 10000 = 500
        BigDecimal result = feeCalculatorService.calculateOriginationFees(productId, BigDecimal.valueOf(10000));

        assertThat(result).isEqualByComparingTo("500.00");
    }

    @Test
    void calculateDailyFee_percentageFee_returnsCorrectDailyAmount() {
        ProductFee fee = buildFee(FeeType.DAILY_FEE, CalculationType.PERCENTAGE, "0.10", 0);
        when(feeRepository.findByProductIdAndFeeType(productId, FeeType.DAILY_FEE)).thenReturn(List.of(fee));

        // 0.1% of 50000 = 50
        BigDecimal result = feeCalculatorService.calculateDailyFee(productId, BigDecimal.valueOf(50000));

        assertThat(result).isEqualByComparingTo("50.00");
    }

    @Test
    void calculateLateFees_beforeTriggerDays_returnsZero() {
        ProductFee fee = buildFee(FeeType.LATE_FEE, CalculationType.FIXED, "500.00", 3);
        when(feeRepository.findByProductIdAndFeeType(productId, FeeType.LATE_FEE)).thenReturn(List.of(fee));

        // daysOverdue=2, daysAfterDue=3 → no fee yet
        BigDecimal result = feeCalculatorService.calculateLateFees(productId, BigDecimal.valueOf(10000), 2);

        assertThat(result).isEqualByComparingTo("0.00");
    }

    @Test
    void calculateLateFees_onTriggerDay_appliesFee() {
        ProductFee fee = buildFee(FeeType.LATE_FEE, CalculationType.FIXED, "500.00", 3);
        when(feeRepository.findByProductIdAndFeeType(productId, FeeType.LATE_FEE)).thenReturn(List.of(fee));

        // daysOverdue=3, daysAfterDue=3 → fee applies
        BigDecimal result = feeCalculatorService.calculateLateFees(productId, BigDecimal.valueOf(10000), 3);

        assertThat(result).isEqualByComparingTo("500.00");
    }

    @Test
    void calculateLateFees_afterTriggerDay_appliesFee() {
        ProductFee fee = buildFee(FeeType.LATE_FEE, CalculationType.PERCENTAGE, "2.00", 5);
        when(feeRepository.findByProductIdAndFeeType(productId, FeeType.LATE_FEE)).thenReturn(List.of(fee));

        // daysOverdue=10, daysAfterDue=5, 2% of 20000 = 400
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
```

- [ ] **Step 8: Run FeeCalculator tests**

```bash
mvn test -Dtest="FeeCalculatorServiceTest" -q
```

Expected: `Tests run: 6, Failures: 0, Errors: 0`

- [ ] **Step 9: Create InstallmentGeneratorService**

```java
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

    /**
     * Generates equal installments for a loan.
     * Principal is split equally; last installment absorbs rounding remainder.
     *
     * @param loan             the parent loan (must have principalAmount, productId set)
     * @param numInstallments  number of installments to generate
     * @param tenureType       DAYS or MONTHS — determines spacing between installments
     * @param tenureValue      value used for spacing (e.g. 30 DAYS or 1 MONTH per installment)
     * @param originationDate  loan disbursement date; first installment due = origination + 1 period
     * @param feePerInstallment fee amount per installment (from FeeCalculatorService, split evenly)
     */
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

            installments.add(LoanInstallment.builder()
                    .loan(loan)
                    .installmentNumber(i)
                    .principalAmount(thisPrincipal)
                    .feeAmount(feePerInstallment)
                    .totalAmount(total)
                    .outstandingAmount(total)
                    .dueDate(dueDate)
                    .status(InstallmentStatus.PENDING)
                    .build());
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
```

- [ ] **Step 10: Write InstallmentGenerator tests first**

Create `src/test/java/com/tezza/lending/loan/InstallmentGeneratorServiceTest.java`:

```java
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
        // 30000 / 3 = 10000 each
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

        // total = principal(10000) + fee(500) = 10500
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
        // 10001 / 3 = 3333.67 each; last = 10001 - (3333.67 * 2) = 3333.66
        Loan loan = buildLoan(BigDecimal.valueOf(10001));
        List<LoanInstallment> installments = service.generate(
                loan, 3, TenureType.DAYS, 30, originationDate, BigDecimal.ZERO);

        BigDecimal sum = installments.stream()
                .map(LoanInstallment::getPrincipalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(sum).isEqualByComparingTo("10001.00");
    }
}
```

- [ ] **Step 11: Run InstallmentGenerator tests**

```bash
mvn test -Dtest="InstallmentGeneratorServiceTest" -q
```

Expected: `Tests run: 7, Failures: 0, Errors: 0`

- [ ] **Step 12: Create DTOs**

```java
// LoanRequest.java
package com.tezza.lending.loan.api.dto;

import com.tezza.lending.loan.internal.entity.enums.BillingCycleType;
import com.tezza.lending.loan.internal.entity.enums.LoanType;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class LoanRequest {
    @NotNull UUID customerId;
    @NotNull UUID productId;
    @NotNull @DecimalMin("0.01") BigDecimal amount;
    @NotNull LoanType loanType;
    @NotNull BillingCycleType billingCycleType;
    @Min(1) Integer numInstallments; // required when loanType=INSTALLMENT
    LocalDate consolidatedDueDate;   // required when billingCycleType=CONSOLIDATED
}
```

```java
// LoanResponse.java
package com.tezza.lending.loan.api.dto;

import com.tezza.lending.loan.internal.entity.Loan;
import com.tezza.lending.loan.internal.entity.enums.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class LoanResponse {
    private UUID id;
    private String loanNumber;
    private UUID customerId;
    private UUID productId;
    private BigDecimal principalAmount;
    private BigDecimal outstandingBalance;
    private LoanStatus status;
    private LoanType loanType;
    private BillingCycleType billingCycleType;
    private LocalDate dueDate;
    private LocalDate consolidatedDueDate;
    private LocalDateTime disbursedAt;
    private LocalDateTime closedAt;

    public static LoanResponse from(Loan l) {
        LoanResponse r = new LoanResponse();
        r.setId(l.getId());
        r.setLoanNumber(l.getLoanNumber());
        r.setCustomerId(l.getCustomerId());
        r.setProductId(l.getProductId());
        r.setPrincipalAmount(l.getPrincipalAmount());
        r.setOutstandingBalance(l.getOutstandingBalance());
        r.setStatus(l.getStatus());
        r.setLoanType(l.getLoanType());
        r.setBillingCycleType(l.getBillingCycleType());
        r.setDueDate(l.getDueDate());
        r.setConsolidatedDueDate(l.getConsolidatedDueDate());
        r.setDisbursedAt(l.getDisbursedAt());
        r.setClosedAt(l.getClosedAt());
        return r;
    }
}
```

```java
// InstallmentResponse.java
package com.tezza.lending.loan.api.dto;

import com.tezza.lending.loan.internal.entity.LoanInstallment;
import com.tezza.lending.loan.internal.entity.enums.InstallmentStatus;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class InstallmentResponse {
    private UUID id;
    private int installmentNumber;
    private BigDecimal principalAmount;
    private BigDecimal feeAmount;
    private BigDecimal totalAmount;
    private BigDecimal outstandingAmount;
    private LocalDate dueDate;
    private LocalDateTime paidAt;
    private InstallmentStatus status;

    public static InstallmentResponse from(LoanInstallment i) {
        InstallmentResponse r = new InstallmentResponse();
        r.setId(i.getId());
        r.setInstallmentNumber(i.getInstallmentNumber());
        r.setPrincipalAmount(i.getPrincipalAmount());
        r.setFeeAmount(i.getFeeAmount());
        r.setTotalAmount(i.getTotalAmount());
        r.setOutstandingAmount(i.getOutstandingAmount());
        r.setDueDate(i.getDueDate());
        r.setPaidAt(i.getPaidAt());
        r.setStatus(i.getStatus());
        return r;
    }
}
```

- [ ] **Step 13: Create LoanService interface**

```java
package com.tezza.lending.loan.api;

import com.tezza.lending.loan.api.dto.*;
import com.tezza.lending.loan.internal.entity.enums.LoanStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface LoanService {
    LoanResponse disburseLoan(LoanRequest request);
    LoanResponse getLoan(UUID id);
    Page<LoanResponse> listLoans(LoanStatus status, Pageable pageable);
    Page<LoanResponse> getCustomerLoans(UUID customerId, Pageable pageable);
    List<InstallmentResponse> getInstallments(UUID loanId);
    LoanResponse cancelLoan(UUID id);
    LoanResponse writeOffLoan(UUID id);
}
```

- [ ] **Step 14: Create LoanServiceImpl**

```java
package com.tezza.lending.loan.internal.service;

import com.tezza.lending.customer.internal.entity.CustomerLoanLimit;
import com.tezza.lending.customer.internal.repository.CustomerLoanLimitRepository;
import com.tezza.lending.customer.internal.repository.CustomerRepository;
import com.tezza.lending.loan.api.LoanService;
import com.tezza.lending.loan.api.dto.*;
import com.tezza.lending.loan.api.event.LoanCreatedEvent;
import com.tezza.lending.loan.internal.entity.Loan;
import com.tezza.lending.loan.internal.entity.LoanInstallment;
import com.tezza.lending.loan.internal.entity.enums.LoanStatus;
import com.tezza.lending.loan.internal.entity.enums.LoanType;
import com.tezza.lending.loan.internal.repository.LoanInstallmentRepository;
import com.tezza.lending.loan.internal.repository.LoanRepository;
import com.tezza.lending.product.internal.entity.LoanProduct;
import com.tezza.lending.product.internal.repository.LoanProductRepository;
import com.tezza.lending.shared.exception.BusinessException;
import com.tezza.lending.shared.exception.ResourceNotFoundException;
import com.tezza.lending.shared.util.LoanNumberGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class LoanServiceImpl implements LoanService {

    private final LoanRepository loanRepository;
    private final LoanInstallmentRepository installmentRepository;
    private final LoanProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final CustomerLoanLimitRepository loanLimitRepository;
    private final FeeCalculatorService feeCalculatorService;
    private final InstallmentGeneratorService installmentGeneratorService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public LoanResponse disburseLoan(LoanRequest request) {
        // Validate customer exists
        customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer", request.getCustomerId().toString()));

        // Validate product active
        LoanProduct product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("LoanProduct", request.getProductId().toString()));
        if (!product.isActive()) {
            throw new BusinessException("Loan product is not active");
        }

        // Validate amount within product bounds
        if (request.getAmount().compareTo(product.getMinAmount()) < 0 ||
            request.getAmount().compareTo(product.getMaxAmount()) > 0) {
            throw new BusinessException(String.format(
                "Loan amount must be between %s and %s", product.getMinAmount(), product.getMaxAmount()));
        }

        // Validate customer limit
        CustomerLoanLimit limit = loanLimitRepository.findByCustomerId(request.getCustomerId())
                .orElseThrow(() -> new BusinessException("No loan limit configured for customer"));
        if (request.getAmount().compareTo(limit.getCurrentLimit()) > 0) {
            throw new BusinessException("Requested amount exceeds customer loan limit of " + limit.getCurrentLimit());
        }

        // Validate installment request
        if (request.getLoanType() == LoanType.INSTALLMENT &&
            (request.getNumInstallments() == null || request.getNumInstallments() < 1)) {
            throw new BusinessException("numInstallments required for INSTALLMENT loan type");
        }

        // Calculate due date
        LocalDate disbursementDate = LocalDate.now();
        LocalDate dueDate = calculateDueDate(disbursementDate, product, request);

        // Build loan
        Loan loan = Loan.builder()
                .loanNumber(LoanNumberGenerator.generate())
                .customerId(request.getCustomerId())
                .productId(request.getProductId())
                .principalAmount(request.getAmount())
                .disbursedAmount(request.getAmount())
                .outstandingBalance(request.getAmount())
                .status(LoanStatus.OPEN)
                .loanType(request.getLoanType())
                .billingCycleType(request.getBillingCycleType())
                .consolidatedDueDate(request.getConsolidatedDueDate())
                .dueDate(dueDate)
                .disbursedAt(LocalDateTime.now())
                .build();

        // Generate installments if INSTALLMENT type
        if (request.getLoanType() == LoanType.INSTALLMENT) {
            BigDecimal serviceFeeTotal = feeCalculatorService.calculateOriginationFees(
                    product.getId(), request.getAmount());
            BigDecimal feePerInstallment = serviceFeeTotal.divide(
                    BigDecimal.valueOf(request.getNumInstallments()), 2, java.math.RoundingMode.HALF_UP);
            List<LoanInstallment> installments = installmentGeneratorService.generate(
                    loan, request.getNumInstallments(), product.getTenureType(),
                    product.getTenureValue(), disbursementDate, feePerInstallment);
            loan.getInstallments().addAll(installments);
        }

        Loan saved = loanRepository.save(loan);
        log.info("Loan disbursed: {} for customer {}", saved.getLoanNumber(), request.getCustomerId());

        // Add origination fee to outstanding balance for LUMP_SUM
        if (request.getLoanType() == LoanType.LUMP_SUM) {
            BigDecimal originationFee = feeCalculatorService.calculateOriginationFees(
                    product.getId(), request.getAmount());
            saved.setOutstandingBalance(saved.getOutstandingBalance().add(originationFee));
            saved = loanRepository.save(saved);
        }

        eventPublisher.publishEvent(new LoanCreatedEvent(
                saved.getId(), saved.getCustomerId(),
                saved.getLoanNumber(), saved.getPrincipalAmount(), saved.getDueDate()));

        return LoanResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public LoanResponse getLoan(UUID id) {
        return loanRepository.findById(id)
                .map(LoanResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", id.toString()));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LoanResponse> listLoans(LoanStatus status, Pageable pageable) {
        if (status != null) {
            return loanRepository.findByStatus(status, pageable).map(LoanResponse::from);
        }
        return loanRepository.findAll(pageable).map(LoanResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LoanResponse> getCustomerLoans(UUID customerId, Pageable pageable) {
        return loanRepository.findByCustomerId(customerId, pageable).map(LoanResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InstallmentResponse> getInstallments(UUID loanId) {
        return installmentRepository.findByLoanIdOrderByInstallmentNumberAsc(loanId)
                .stream().map(InstallmentResponse::from).toList();
    }

    @Override
    public LoanResponse cancelLoan(UUID id) {
        Loan loan = findLoan(id);
        if (loan.getStatus() != LoanStatus.OPEN) {
            throw new BusinessException("Only OPEN loans can be cancelled. Current status: " + loan.getStatus());
        }
        loan.setStatus(LoanStatus.CANCELLED);
        loan.setClosedAt(LocalDateTime.now());
        return LoanResponse.from(loanRepository.save(loan));
    }

    @Override
    public LoanResponse writeOffLoan(UUID id) {
        Loan loan = findLoan(id);
        if (loan.getStatus() != LoanStatus.OVERDUE) {
            throw new BusinessException("Only OVERDUE loans can be written off. Current status: " + loan.getStatus());
        }
        loan.setStatus(LoanStatus.WRITTEN_OFF);
        loan.setClosedAt(LocalDateTime.now());
        return LoanResponse.from(loanRepository.save(loan));
    }

    private Loan findLoan(UUID id) {
        return loanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Loan", id.toString()));
    }

    private LocalDate calculateDueDate(LocalDate disbursementDate, LoanProduct product, LoanRequest request) {
        if (request.getBillingCycleType() == com.tezza.lending.loan.internal.entity.enums.BillingCycleType.CONSOLIDATED
            && request.getConsolidatedDueDate() != null) {
            return request.getConsolidatedDueDate();
        }
        return switch (product.getTenureType()) {
            case DAYS -> disbursementDate.plusDays(product.getTenureValue());
            case MONTHS -> disbursementDate.plusMonths(product.getTenureValue());
        };
    }
}
```

- [ ] **Step 15: Write LoanService tests**

Create `src/test/java/com/tezza/lending/loan/LoanServiceTest.java`:

```java
package com.tezza.lending.loan;

import com.tezza.lending.customer.internal.entity.Customer;
import com.tezza.lending.customer.internal.entity.CustomerLoanLimit;
import com.tezza.lending.customer.internal.repository.CustomerLoanLimitRepository;
import com.tezza.lending.customer.internal.repository.CustomerRepository;
import com.tezza.lending.loan.api.dto.LoanRequest;
import com.tezza.lending.loan.api.dto.LoanResponse;
import com.tezza.lending.loan.internal.entity.Loan;
import com.tezza.lending.loan.internal.entity.enums.*;
import com.tezza.lending.loan.internal.repository.LoanInstallmentRepository;
import com.tezza.lending.loan.internal.repository.LoanRepository;
import com.tezza.lending.loan.internal.service.FeeCalculatorService;
import com.tezza.lending.loan.internal.service.InstallmentGeneratorService;
import com.tezza.lending.loan.internal.service.LoanServiceImpl;
import com.tezza.lending.product.internal.entity.LoanProduct;
import com.tezza.lending.product.internal.entity.enums.TenureType;
import com.tezza.lending.product.internal.repository.LoanProductRepository;
import com.tezza.lending.shared.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock LoanRepository loanRepository;
    @Mock LoanInstallmentRepository installmentRepository;
    @Mock LoanProductRepository productRepository;
    @Mock CustomerRepository customerRepository;
    @Mock CustomerLoanLimitRepository loanLimitRepository;
    @Mock FeeCalculatorService feeCalculatorService;
    @Mock InstallmentGeneratorService installmentGeneratorService;
    @Mock ApplicationEventPublisher eventPublisher;
    @InjectMocks LoanServiceImpl loanService;

    private UUID customerId;
    private UUID productId;
    private LoanProduct product;
    private CustomerLoanLimit loanLimit;
    private LoanRequest validRequest;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        productId = UUID.randomUUID();

        product = LoanProduct.builder()
                .id(productId)
                .tenureValue(30)
                .tenureType(TenureType.DAYS)
                .minAmount(BigDecimal.valueOf(1000))
                .maxAmount(BigDecimal.valueOf(50000))
                .active(true)
                .fees(new ArrayList<>())
                .build();

        loanLimit = CustomerLoanLimit.builder()
                .customer(Customer.builder().id(customerId).build())
                .currentLimit(BigDecimal.valueOf(50000))
                .build();

        validRequest = new LoanRequest();
        validRequest.setCustomerId(customerId);
        validRequest.setProductId(productId);
        validRequest.setAmount(BigDecimal.valueOf(10000));
        validRequest.setLoanType(LoanType.LUMP_SUM);
        validRequest.setBillingCycleType(BillingCycleType.INDIVIDUAL);
    }

    @Test
    void disburseLoan_createsOpenLoan() {
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(Customer.builder().id(customerId).build()));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(loanLimitRepository.findByCustomerId(customerId)).thenReturn(Optional.of(loanLimit));
        when(feeCalculatorService.calculateOriginationFees(any(), any())).thenReturn(BigDecimal.ZERO);
        when(loanRepository.save(any())).thenAnswer(inv -> {
            Loan l = inv.getArgument(0);
            l.setInstallments(new ArrayList<>());
            return l;
        });

        LoanResponse result = loanService.disburseLoan(validRequest);

        assertThat(result.getStatus()).isEqualTo(LoanStatus.OPEN);
        assertThat(result.getPrincipalAmount()).isEqualByComparingTo("10000.00");
    }

    @Test
    void disburseLoan_amountExceedsCustomerLimit_throwsBusinessException() {
        validRequest.setAmount(BigDecimal.valueOf(60000)); // exceeds 50000 limit
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(Customer.builder().id(customerId).build()));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(loanLimitRepository.findByCustomerId(customerId)).thenReturn(Optional.of(loanLimit));

        assertThatThrownBy(() -> loanService.disburseLoan(validRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("exceeds customer loan limit");
    }

    @Test
    void disburseLoan_inactiveProduct_throwsBusinessException() {
        product.setActive(false);
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(Customer.builder().id(customerId).build()));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> loanService.disburseLoan(validRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("not active");
    }

    @Test
    void disburseLoan_installmentType_requiresNumInstallments() {
        validRequest.setLoanType(LoanType.INSTALLMENT);
        validRequest.setNumInstallments(null);
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(Customer.builder().id(customerId).build()));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(loanLimitRepository.findByCustomerId(customerId)).thenReturn(Optional.of(loanLimit));

        assertThatThrownBy(() -> loanService.disburseLoan(validRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("numInstallments required");
    }

    @Test
    void cancelLoan_openLoan_succeeds() {
        Loan loan = Loan.builder().id(UUID.randomUUID()).status(LoanStatus.OPEN).installments(new ArrayList<>()).build();
        when(loanRepository.findById(loan.getId())).thenReturn(Optional.of(loan));
        when(loanRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LoanResponse result = loanService.cancelLoan(loan.getId());

        assertThat(result.getStatus()).isEqualTo(LoanStatus.CANCELLED);
    }

    @Test
    void cancelLoan_closedLoan_throwsBusinessException() {
        Loan loan = Loan.builder().id(UUID.randomUUID()).status(LoanStatus.CLOSED).installments(new ArrayList<>()).build();
        when(loanRepository.findById(loan.getId())).thenReturn(Optional.of(loan));

        assertThatThrownBy(() -> loanService.cancelLoan(loan.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("CLOSED");
    }

    @Test
    void writeOffLoan_overdueLoan_succeeds() {
        Loan loan = Loan.builder().id(UUID.randomUUID()).status(LoanStatus.OVERDUE).installments(new ArrayList<>()).build();
        when(loanRepository.findById(loan.getId())).thenReturn(Optional.of(loan));
        when(loanRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LoanResponse result = loanService.writeOffLoan(loan.getId());

        assertThat(result.getStatus()).isEqualTo(LoanStatus.WRITTEN_OFF);
    }

    @Test
    void writeOffLoan_openLoan_throwsBusinessException() {
        Loan loan = Loan.builder().id(UUID.randomUUID()).status(LoanStatus.OPEN).installments(new ArrayList<>()).build();
        when(loanRepository.findById(loan.getId())).thenReturn(Optional.of(loan));

        assertThatThrownBy(() -> loanService.writeOffLoan(loan.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("OVERDUE");
    }
}
```

- [ ] **Step 16: Run LoanService tests**

```bash
mvn test -Dtest="LoanServiceTest" -q
```

Expected: `Tests run: 7, Failures: 0, Errors: 0`

- [ ] **Step 17: Create LoanController**

```java
package com.tezza.lending.loan.web;

import com.tezza.lending.loan.api.LoanService;
import com.tezza.lending.loan.api.dto.*;
import com.tezza.lending.loan.internal.entity.enums.LoanStatus;
import com.tezza.lending.shared.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/loans")
@RequiredArgsConstructor
@Tag(name = "Loans")
@SecurityRequirement(name = "Bearer Authentication")
public class LoanController {

    private final LoanService loanService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
    @Operation(summary = "Apply and disburse a loan")
    public ResponseEntity<ApiResponse<LoanResponse>> disburse(@Valid @RequestBody LoanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Loan disbursed", loanService.disburseLoan(request)));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List loans with optional status filter")
    public ResponseEntity<ApiResponse<Page<LoanResponse>>> list(
            @RequestParam(required = false) LoanStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(loanService.listLoans(status, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
    @Operation(summary = "Get loan by ID")
    public ResponseEntity<ApiResponse<LoanResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(loanService.getLoan(id)));
    }

    @GetMapping("/{id}/installments")
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
    @Operation(summary = "Get loan installment schedule")
    public ResponseEntity<ApiResponse<List<InstallmentResponse>>> getInstallments(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(loanService.getInstallments(id)));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cancel an open loan")
    public ResponseEntity<ApiResponse<LoanResponse>> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Loan cancelled", loanService.cancelLoan(id)));
    }

    @PostMapping("/{id}/write-off")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Write off an overdue loan")
    public ResponseEntity<ApiResponse<LoanResponse>> writeOff(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Loan written off", loanService.writeOffLoan(id)));
    }
}
```

- [ ] **Step 18: Commit**

```bash
git add src/main/java/com/tezza/lending/loan/ \
        src/test/java/com/tezza/lending/loan/
git commit -m "feat: add loan module — disbursement, installments, fee calc, lifecycle, sweep-ready"
```

---

## Task 8: Loan Sweep Jobs

**Files:**
- Create: `src/main/java/com/tezza/lending/loan/internal/scheduler/OverdueSweepJob.java`
- Create: `src/main/java/com/tezza/lending/loan/internal/scheduler/DailyFeeSweepJob.java`
- Create: `src/main/java/com/tezza/lending/loan/internal/scheduler/DueDateReminderJob.java`
- Test: `src/test/java/com/tezza/lending/loan/OverdueSweepJobTest.java`
- Test: `src/test/java/com/tezza/lending/loan/DailyFeeSweepJobTest.java`
- Test: `src/test/java/com/tezza/lending/loan/DueDateReminderJobTest.java`

- [ ] **Step 1: Create OverdueSweepJob**

```java
package com.tezza.lending.loan.internal.scheduler;

import com.tezza.lending.loan.api.event.LoanOverdueEvent;
import com.tezza.lending.loan.internal.entity.Loan;
import com.tezza.lending.loan.internal.entity.LoanInstallment;
import com.tezza.lending.loan.internal.entity.enums.InstallmentStatus;
import com.tezza.lending.loan.internal.entity.enums.LoanStatus;
import com.tezza.lending.loan.internal.repository.LoanRepository;
import com.tezza.lending.loan.internal.service.FeeCalculatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OverdueSweepJob {

    private final LoanRepository loanRepository;
    private final FeeCalculatorService feeCalculatorService;
    private final ApplicationEventPublisher eventPublisher;

    @Scheduled(cron = "0 5 0 * * *")
    @Transactional
    public void runOverdueSweep() {
        LocalDate today = LocalDate.now();
        List<Loan> overdueLoans = loanRepository.findOverdueLoans(today);
        log.info("OverdueSweepJob: processing {} overdue loans", overdueLoans.size());

        for (Loan loan : overdueLoans) {
            int daysOverdue = (int) (today.toEpochDay() - loan.getDueDate().toEpochDay());

            // Apply late fee
            var lateFee = feeCalculatorService.calculateLateFees(
                    loan.getProductId(), loan.getOutstandingBalance(), daysOverdue);
            if (lateFee.signum() > 0) {
                loan.setOutstandingBalance(loan.getOutstandingBalance().add(lateFee));
                log.info("Applied late fee {} to loan {}", lateFee, loan.getLoanNumber());
            }

            // Mark overdue installments
            for (LoanInstallment installment : loan.getInstallments()) {
                if (installment.getStatus() == InstallmentStatus.PENDING &&
                    installment.getDueDate().isBefore(today)) {
                    installment.setStatus(InstallmentStatus.OVERDUE);
                }
            }

            // Transition loan status
            loan.setStatus(LoanStatus.OVERDUE);
            loanRepository.save(loan);

            eventPublisher.publishEvent(new LoanOverdueEvent(
                    loan.getId(), loan.getCustomerId(),
                    loan.getLoanNumber(), loan.getOutstandingBalance()));
        }
        log.info("OverdueSweepJob: completed, {} loans marked overdue", overdueLoans.size());
    }
}
```

- [ ] **Step 2: Create DailyFeeSweepJob**

```java
package com.tezza.lending.loan.internal.scheduler;

import com.tezza.lending.loan.internal.entity.Loan;
import com.tezza.lending.loan.internal.repository.LoanRepository;
import com.tezza.lending.loan.internal.service.FeeCalculatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DailyFeeSweepJob {

    private final LoanRepository loanRepository;
    private final FeeCalculatorService feeCalculatorService;

    @Scheduled(cron = "0 10 0 * * *")
    @Transactional
    public void runDailyFeeSweep() {
        List<Loan> loans = loanRepository.findLoansWithDailyFee();
        log.info("DailyFeeSweepJob: accruing daily fees on {} loans", loans.size());

        for (Loan loan : loans) {
            BigDecimal dailyFee = feeCalculatorService.calculateDailyFee(
                    loan.getProductId(), loan.getOutstandingBalance());
            if (dailyFee.signum() > 0) {
                loan.setOutstandingBalance(loan.getOutstandingBalance().add(dailyFee));
                loanRepository.save(loan);
                log.debug("Accrued daily fee {} on loan {}", dailyFee, loan.getLoanNumber());
            }
        }
    }
}
```

- [ ] **Step 3: Create DueDateReminderJob**

```java
package com.tezza.lending.loan.internal.scheduler;

import com.tezza.lending.loan.api.event.DueDateReminderEvent;
import com.tezza.lending.loan.internal.entity.Loan;
import com.tezza.lending.loan.internal.repository.LoanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DueDateReminderJob {

    private final LoanRepository loanRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${tezza.notification.due-reminder-days-before:3}")
    private int daysBeforeDue;

    @Scheduled(cron = "0 0 8 * * *")
    @Transactional(readOnly = true)
    public void runDueDateReminderSweep() {
        LocalDate targetDate = LocalDate.now().plusDays(daysBeforeDue);
        List<Loan> loans = loanRepository.findLoansDueOn(targetDate);
        log.info("DueDateReminderJob: sending reminders for {} loans due on {}", loans.size(), targetDate);

        for (Loan loan : loans) {
            eventPublisher.publishEvent(new DueDateReminderEvent(
                    loan.getId(), loan.getCustomerId(),
                    loan.getLoanNumber(), loan.getOutstandingBalance(), loan.getDueDate()));
        }
    }
}
```

- [ ] **Step 4: Write OverdueSweepJob tests**

Create `src/test/java/com/tezza/lending/loan/OverdueSweepJobTest.java`:

```java
package com.tezza.lending.loan;

import com.tezza.lending.loan.api.event.LoanOverdueEvent;
import com.tezza.lending.loan.internal.entity.Loan;
import com.tezza.lending.loan.internal.entity.enums.LoanStatus;
import com.tezza.lending.loan.internal.repository.LoanRepository;
import com.tezza.lending.loan.internal.scheduler.OverdueSweepJob;
import com.tezza.lending.loan.internal.service.FeeCalculatorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OverdueSweepJobTest {

    @Mock LoanRepository loanRepository;
    @Mock FeeCalculatorService feeCalculatorService;
    @Mock ApplicationEventPublisher eventPublisher;
    @InjectMocks OverdueSweepJob overdueSweepJob;

    @Test
    void runOverdueSweep_overdueLoans_markedOverdueAndEventPublished() {
        Loan loan = Loan.builder()
                .id(UUID.randomUUID())
                .productId(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .loanNumber("TZ-2026-00000001")
                .status(LoanStatus.OPEN)
                .dueDate(LocalDate.now().minusDays(5))
                .outstandingBalance(BigDecimal.valueOf(10000))
                .installments(new ArrayList<>())
                .build();

        when(loanRepository.findOverdueLoans(any())).thenReturn(List.of(loan));
        when(feeCalculatorService.calculateLateFees(any(), any(), anyInt())).thenReturn(BigDecimal.valueOf(500));
        when(loanRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        overdueSweepJob.runOverdueSweep();

        assertThat(loan.getStatus()).isEqualTo(LoanStatus.OVERDUE);
        assertThat(loan.getOutstandingBalance()).isEqualByComparingTo("10500.00"); // 10000 + 500 late fee

        ArgumentCaptor<LoanOverdueEvent> captor = ArgumentCaptor.forClass(LoanOverdueEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().loanId()).isEqualTo(loan.getId());
    }

    @Test
    void runOverdueSweep_noOverdueLoans_noEventsPublished() {
        when(loanRepository.findOverdueLoans(any())).thenReturn(List.of());

        overdueSweepJob.runOverdueSweep();

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void runOverdueSweep_noLateFee_balanceUnchanged() {
        Loan loan = Loan.builder()
                .id(UUID.randomUUID())
                .productId(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .loanNumber("TZ-2026-00000002")
                .status(LoanStatus.OPEN)
                .dueDate(LocalDate.now().minusDays(1))
                .outstandingBalance(BigDecimal.valueOf(10000))
                .installments(new ArrayList<>())
                .build();

        when(loanRepository.findOverdueLoans(any())).thenReturn(List.of(loan));
        when(feeCalculatorService.calculateLateFees(any(), any(), anyInt())).thenReturn(BigDecimal.ZERO);
        when(loanRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        overdueSweepJob.runOverdueSweep();

        assertThat(loan.getOutstandingBalance()).isEqualByComparingTo("10000.00");
    }
}
```

- [ ] **Step 5: Write DailyFeeSweepJob tests**

Create `src/test/java/com/tezza/lending/loan/DailyFeeSweepJobTest.java`:

```java
package com.tezza.lending.loan;

import com.tezza.lending.loan.internal.entity.Loan;
import com.tezza.lending.loan.internal.entity.enums.LoanStatus;
import com.tezza.lending.loan.internal.repository.LoanRepository;
import com.tezza.lending.loan.internal.scheduler.DailyFeeSweepJob;
import com.tezza.lending.loan.internal.service.FeeCalculatorService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DailyFeeSweepJobTest {

    @Mock LoanRepository loanRepository;
    @Mock FeeCalculatorService feeCalculatorService;
    @InjectMocks DailyFeeSweepJob dailyFeeSweepJob;

    @Test
    void runDailyFeeSweep_openLoanWithDailyFee_balanceIncreases() {
        Loan loan = Loan.builder()
                .id(UUID.randomUUID())
                .productId(UUID.randomUUID())
                .status(LoanStatus.OPEN)
                .outstandingBalance(BigDecimal.valueOf(50000))
                .installments(new ArrayList<>())
                .build();

        when(loanRepository.findLoansWithDailyFee()).thenReturn(List.of(loan));
        when(feeCalculatorService.calculateDailyFee(any(), any())).thenReturn(BigDecimal.valueOf(50));
        when(loanRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        dailyFeeSweepJob.runDailyFeeSweep();

        assertThat(loan.getOutstandingBalance()).isEqualByComparingTo("50050.00");
        verify(loanRepository).save(loan);
    }

    @Test
    void runDailyFeeSweep_zeroDailyFee_loanNotSaved() {
        Loan loan = Loan.builder()
                .id(UUID.randomUUID())
                .productId(UUID.randomUUID())
                .status(LoanStatus.OPEN)
                .outstandingBalance(BigDecimal.valueOf(50000))
                .installments(new ArrayList<>())
                .build();

        when(loanRepository.findLoansWithDailyFee()).thenReturn(List.of(loan));
        when(feeCalculatorService.calculateDailyFee(any(), any())).thenReturn(BigDecimal.ZERO);

        dailyFeeSweepJob.runDailyFeeSweep();

        verify(loanRepository, never()).save(any());
    }
}
```

- [ ] **Step 6: Write DueDateReminderJob tests**

Create `src/test/java/com/tezza/lending/loan/DueDateReminderJobTest.java`:

```java
package com.tezza.lending.loan;

import com.tezza.lending.loan.api.event.DueDateReminderEvent;
import com.tezza.lending.loan.internal.entity.Loan;
import com.tezza.lending.loan.internal.entity.enums.LoanStatus;
import com.tezza.lending.loan.internal.repository.LoanRepository;
import com.tezza.lending.loan.internal.scheduler.DueDateReminderJob;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DueDateReminderJobTest {

    @Mock LoanRepository loanRepository;
    @Mock ApplicationEventPublisher eventPublisher;
    @InjectMocks DueDateReminderJob dueDateReminderJob;

    @Test
    void runDueDateReminderSweep_loanDueIn3Days_eventPublished() {
        ReflectionTestUtils.setField(dueDateReminderJob, "daysBeforeDue", 3);
        LocalDate targetDate = LocalDate.now().plusDays(3);

        Loan loan = Loan.builder()
                .id(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .loanNumber("TZ-2026-00000001")
                .status(LoanStatus.OPEN)
                .dueDate(targetDate)
                .outstandingBalance(BigDecimal.valueOf(10000))
                .installments(new ArrayList<>())
                .build();

        when(loanRepository.findLoansDueOn(targetDate)).thenReturn(List.of(loan));

        dueDateReminderJob.runDueDateReminderSweep();

        ArgumentCaptor<DueDateReminderEvent> captor = ArgumentCaptor.forClass(DueDateReminderEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().loanId()).isEqualTo(loan.getId());
        assertThat(captor.getValue().dueDate()).isEqualTo(targetDate);
    }

    @Test
    void runDueDateReminderSweep_noLoansDueSoon_noEventsPublished() {
        ReflectionTestUtils.setField(dueDateReminderJob, "daysBeforeDue", 3);
        when(loanRepository.findLoansDueOn(any())).thenReturn(List.of());

        dueDateReminderJob.runDueDateReminderSweep();

        verify(eventPublisher, never()).publishEvent(any());
    }
}
```

- [ ] **Step 7: Run all sweep job tests**

```bash
mvn test -Dtest="OverdueSweepJobTest,DailyFeeSweepJobTest,DueDateReminderJobTest" -q
```

Expected: `Tests run: 7, Failures: 0, Errors: 0`

- [ ] **Step 8: Commit**

```bash
git add src/main/java/com/tezza/lending/loan/internal/scheduler/ \
        src/test/java/com/tezza/lending/loan/Overdue* \
        src/test/java/com/tezza/lending/loan/Daily* \
        src/test/java/com/tezza/lending/loan/DueDate*
git commit -m "feat: add loan sweep jobs — overdue detection, daily fees, due date reminders"
```

---

## Task 9: Repayment Module

**Files:**
- Create: `src/main/java/com/tezza/lending/repayment/internal/entity/Repayment.java`
- Create: `src/main/java/com/tezza/lending/repayment/internal/entity/enums/PaymentChannel.java`
- Create: `src/main/java/com/tezza/lending/repayment/internal/repository/RepaymentRepository.java`
- Create: `src/main/java/com/tezza/lending/repayment/internal/service/RepaymentServiceImpl.java`
- Create: `src/main/java/com/tezza/lending/repayment/api/RepaymentService.java`
- Create: `src/main/java/com/tezza/lending/repayment/api/dto/RepaymentRequest.java`
- Create: `src/main/java/com/tezza/lending/repayment/api/dto/RepaymentResponse.java`
- Create: `src/main/java/com/tezza/lending/repayment/web/RepaymentController.java`
- Test: `src/test/java/com/tezza/lending/repayment/RepaymentServiceTest.java`

- [ ] **Step 1: Create PaymentChannel enum and Repayment entity**

```java
// PaymentChannel.java
package com.tezza.lending.repayment.internal.entity.enums;
public enum PaymentChannel { MPESA, BANK, CASH }
```

```java
// Repayment.java
package com.tezza.lending.repayment.internal.entity;

import com.tezza.lending.repayment.internal.entity.enums.PaymentChannel;
import com.tezza.lending.shared.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "REPAYMENTS")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Repayment extends BaseAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "ID")
    private UUID id;

    @Column(name = "LOAN_ID", nullable = false)
    private UUID loanId;

    @Column(name = "AMOUNT", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "REFERENCE", nullable = false, unique = true, length = 100)
    private String reference;

    @Enumerated(EnumType.STRING)
    @Column(name = "CHANNEL", nullable = false, length = 10)
    private PaymentChannel channel;

    @Column(name = "PROCESSED_AT", nullable = false)
    private LocalDateTime processedAt;
}
```

- [ ] **Step 2: Create RepaymentRepository**

```java
package com.tezza.lending.repayment.internal.repository;

import com.tezza.lending.repayment.internal.entity.Repayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RepaymentRepository extends JpaRepository<Repayment, UUID> {
    List<Repayment> findByLoanIdOrderByProcessedAtDesc(UUID loanId);
    boolean existsByReference(String reference);
}
```

- [ ] **Step 3: Create DTOs**

```java
// RepaymentRequest.java
package com.tezza.lending.repayment.api.dto;

import com.tezza.lending.repayment.internal.entity.enums.PaymentChannel;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class RepaymentRequest {
    @NotNull UUID loanId;
    @NotNull @DecimalMin("0.01") BigDecimal amount;
    @NotBlank @Size(max = 100) String reference;
    @NotNull PaymentChannel channel;
}
```

```java
// RepaymentResponse.java
package com.tezza.lending.repayment.api.dto;

import com.tezza.lending.repayment.internal.entity.Repayment;
import com.tezza.lending.repayment.internal.entity.enums.PaymentChannel;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class RepaymentResponse {
    private UUID id;
    private UUID loanId;
    private BigDecimal amount;
    private String reference;
    private PaymentChannel channel;
    private LocalDateTime processedAt;

    public static RepaymentResponse from(Repayment r) {
        RepaymentResponse res = new RepaymentResponse();
        res.setId(r.getId());
        res.setLoanId(r.getLoanId());
        res.setAmount(r.getAmount());
        res.setReference(r.getReference());
        res.setChannel(r.getChannel());
        res.setProcessedAt(r.getProcessedAt());
        return res;
    }
}
```

- [ ] **Step 4: Create RepaymentService interface**

```java
package com.tezza.lending.repayment.api;

import com.tezza.lending.repayment.api.dto.RepaymentRequest;
import com.tezza.lending.repayment.api.dto.RepaymentResponse;

import java.util.List;
import java.util.UUID;

public interface RepaymentService {
    RepaymentResponse processRepayment(RepaymentRequest request);
    List<RepaymentResponse> getLoanRepayments(UUID loanId);
}
```

- [ ] **Step 5: Create RepaymentServiceImpl**

```java
package com.tezza.lending.repayment.internal.service;

import com.tezza.lending.loan.api.event.RepaymentReceivedEvent;
import com.tezza.lending.loan.internal.entity.Loan;
import com.tezza.lending.loan.internal.entity.LoanInstallment;
import com.tezza.lending.loan.internal.entity.enums.InstallmentStatus;
import com.tezza.lending.loan.internal.entity.enums.LoanStatus;
import com.tezza.lending.loan.internal.repository.LoanInstallmentRepository;
import com.tezza.lending.loan.internal.repository.LoanRepository;
import com.tezza.lending.repayment.api.RepaymentService;
import com.tezza.lending.repayment.api.dto.RepaymentRequest;
import com.tezza.lending.repayment.api.dto.RepaymentResponse;
import com.tezza.lending.repayment.internal.entity.Repayment;
import com.tezza.lending.repayment.internal.repository.RepaymentRepository;
import com.tezza.lending.shared.exception.BusinessException;
import com.tezza.lending.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class RepaymentServiceImpl implements RepaymentService {

    private final RepaymentRepository repaymentRepository;
    private final LoanRepository loanRepository;
    private final LoanInstallmentRepository installmentRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public RepaymentResponse processRepayment(RepaymentRequest request) {
        if (repaymentRepository.existsByReference(request.getReference())) {
            throw new BusinessException("Duplicate repayment reference: " + request.getReference());
        }

        Loan loan = loanRepository.findById(request.getLoanId())
                .orElseThrow(() -> new ResourceNotFoundException("Loan", request.getLoanId().toString()));

        if (loan.getStatus() != LoanStatus.OPEN && loan.getStatus() != LoanStatus.OVERDUE) {
            throw new BusinessException("Cannot process repayment for loan in status: " + loan.getStatus());
        }

        BigDecimal paymentAmount = request.getAmount();

        // Reduce outstanding balance
        BigDecimal newBalance = loan.getOutstandingBalance().subtract(paymentAmount);
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            newBalance = BigDecimal.ZERO;
        }
        loan.setOutstandingBalance(newBalance);

        // Allocate to installments FIFO (oldest PENDING first)
        allocateToInstallments(loan.getId(), paymentAmount);

        // Close loan if fully paid
        if (newBalance.compareTo(BigDecimal.ZERO) == 0) {
            loan.setStatus(LoanStatus.CLOSED);
            loan.setClosedAt(LocalDateTime.now());
            log.info("Loan {} fully repaid and closed", loan.getLoanNumber());
        } else if (loan.getStatus() == LoanStatus.OVERDUE && newBalance.signum() > 0) {
            loan.setStatus(LoanStatus.OPEN); // revert to open if partially paid and current
        }

        loanRepository.save(loan);

        Repayment repayment = Repayment.builder()
                .loanId(request.getLoanId())
                .amount(paymentAmount)
                .reference(request.getReference())
                .channel(request.getChannel())
                .processedAt(LocalDateTime.now())
                .build();
        repaymentRepository.save(repayment);

        eventPublisher.publishEvent(new RepaymentReceivedEvent(
                loan.getId(), loan.getCustomerId(),
                loan.getLoanNumber(), paymentAmount, loan.getOutstandingBalance()));

        return RepaymentResponse.from(repayment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RepaymentResponse> getLoanRepayments(UUID loanId) {
        return repaymentRepository.findByLoanIdOrderByProcessedAtDesc(loanId)
                .stream().map(RepaymentResponse::from).toList();
    }

    private void allocateToInstallments(UUID loanId, BigDecimal paymentAmount) {
        List<LoanInstallment> pendingInstallments = installmentRepository
                .findByLoanIdAndStatusOrderByDueDateAsc(loanId, InstallmentStatus.PENDING);

        BigDecimal remaining = paymentAmount;
        for (LoanInstallment installment : pendingInstallments) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;

            if (remaining.compareTo(installment.getOutstandingAmount()) >= 0) {
                remaining = remaining.subtract(installment.getOutstandingAmount());
                installment.setOutstandingAmount(BigDecimal.ZERO);
                installment.setStatus(InstallmentStatus.PAID);
                installment.setPaidAt(LocalDateTime.now());
            } else {
                installment.setOutstandingAmount(installment.getOutstandingAmount().subtract(remaining));
                remaining = BigDecimal.ZERO;
            }
            installmentRepository.save(installment);
        }
    }
}
```

- [ ] **Step 6: Create RepaymentController**

```java
package com.tezza.lending.repayment.web;

import com.tezza.lending.repayment.api.RepaymentService;
import com.tezza.lending.repayment.api.dto.RepaymentRequest;
import com.tezza.lending.repayment.api.dto.RepaymentResponse;
import com.tezza.lending.shared.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Repayments")
@SecurityRequirement(name = "Bearer Authentication")
public class RepaymentController {

    private final RepaymentService repaymentService;

    @PostMapping("/api/v1/repayments")
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
    @Operation(summary = "Process a loan repayment")
    public ResponseEntity<ApiResponse<RepaymentResponse>> process(@Valid @RequestBody RepaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Repayment processed", repaymentService.processRepayment(request)));
    }

    @GetMapping("/api/v1/loans/{loanId}/repayments")
    @PreAuthorize("hasAnyRole('ADMIN', 'CUSTOMER')")
    @Operation(summary = "Get repayment history for a loan")
    public ResponseEntity<ApiResponse<List<RepaymentResponse>>> getHistory(@PathVariable UUID loanId) {
        return ResponseEntity.ok(ApiResponse.success(repaymentService.getLoanRepayments(loanId)));
    }
}
```

- [ ] **Step 7: Write RepaymentService tests**

Create `src/test/java/com/tezza/lending/repayment/RepaymentServiceTest.java`:

```java
package com.tezza.lending.repayment;

import com.tezza.lending.loan.api.event.RepaymentReceivedEvent;
import com.tezza.lending.loan.internal.entity.Loan;
import com.tezza.lending.loan.internal.entity.LoanInstallment;
import com.tezza.lending.loan.internal.entity.enums.InstallmentStatus;
import com.tezza.lending.loan.internal.entity.enums.LoanStatus;
import com.tezza.lending.loan.internal.repository.LoanInstallmentRepository;
import com.tezza.lending.loan.internal.repository.LoanRepository;
import com.tezza.lending.repayment.api.dto.RepaymentRequest;
import com.tezza.lending.repayment.api.dto.RepaymentResponse;
import com.tezza.lending.repayment.internal.entity.enums.PaymentChannel;
import com.tezza.lending.repayment.internal.repository.RepaymentRepository;
import com.tezza.lending.repayment.internal.service.RepaymentServiceImpl;
import com.tezza.lending.shared.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RepaymentServiceTest {

    @Mock RepaymentRepository repaymentRepository;
    @Mock LoanRepository loanRepository;
    @Mock LoanInstallmentRepository installmentRepository;
    @Mock ApplicationEventPublisher eventPublisher;
    @InjectMocks RepaymentServiceImpl repaymentService;

    private Loan openLoan;
    private RepaymentRequest validRequest;

    @BeforeEach
    void setUp() {
        openLoan = Loan.builder()
                .id(UUID.randomUUID())
                .customerId(UUID.randomUUID())
                .loanNumber("TZ-2026-00000001")
                .status(LoanStatus.OPEN)
                .outstandingBalance(BigDecimal.valueOf(10000))
                .installments(new ArrayList<>())
                .build();

        validRequest = new RepaymentRequest();
        validRequest.setLoanId(openLoan.getId());
        validRequest.setAmount(BigDecimal.valueOf(3000));
        validRequest.setReference("MPESA-12345");
        validRequest.setChannel(PaymentChannel.MPESA);
    }

    @Test
    void processRepayment_partialPayment_reducesBalance() {
        when(repaymentRepository.existsByReference("MPESA-12345")).thenReturn(false);
        when(loanRepository.findById(openLoan.getId())).thenReturn(Optional.of(openLoan));
        when(installmentRepository.findByLoanIdAndStatusOrderByDueDateAsc(any(), any())).thenReturn(List.of());
        when(repaymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(loanRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        repaymentService.processRepayment(validRequest);

        assertThat(openLoan.getOutstandingBalance()).isEqualByComparingTo("7000.00");
        assertThat(openLoan.getStatus()).isEqualTo(LoanStatus.OPEN);
    }

    @Test
    void processRepayment_fullPayment_closesLoan() {
        validRequest.setAmount(BigDecimal.valueOf(10000));
        when(repaymentRepository.existsByReference(any())).thenReturn(false);
        when(loanRepository.findById(openLoan.getId())).thenReturn(Optional.of(openLoan));
        when(installmentRepository.findByLoanIdAndStatusOrderByDueDateAsc(any(), any())).thenReturn(List.of());
        when(repaymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(loanRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        repaymentService.processRepayment(validRequest);

        assertThat(openLoan.getStatus()).isEqualTo(LoanStatus.CLOSED);
        assertThat(openLoan.getOutstandingBalance()).isEqualByComparingTo("0.00");
    }

    @Test
    void processRepayment_closedLoan_throwsBusinessException() {
        openLoan.setStatus(LoanStatus.CLOSED);
        when(repaymentRepository.existsByReference(any())).thenReturn(false);
        when(loanRepository.findById(openLoan.getId())).thenReturn(Optional.of(openLoan));

        assertThatThrownBy(() -> repaymentService.processRepayment(validRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("CLOSED");
    }

    @Test
    void processRepayment_duplicateReference_throwsBusinessException() {
        when(repaymentRepository.existsByReference("MPESA-12345")).thenReturn(true);

        assertThatThrownBy(() -> repaymentService.processRepayment(validRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Duplicate");
    }

    @Test
    void processRepayment_installmentAllocatedFifo() {
        LoanInstallment inst1 = LoanInstallment.builder()
                .id(UUID.randomUUID()).loanId(openLoan.getId())
                .installmentNumber(1).outstandingAmount(BigDecimal.valueOf(3000))
                .dueDate(LocalDate.now().minusDays(30)).status(InstallmentStatus.PENDING).build();
        LoanInstallment inst2 = LoanInstallment.builder()
                .id(UUID.randomUUID()).loanId(openLoan.getId())
                .installmentNumber(2).outstandingAmount(BigDecimal.valueOf(3000))
                .dueDate(LocalDate.now()).status(InstallmentStatus.PENDING).build();

        when(repaymentRepository.existsByReference(any())).thenReturn(false);
        when(loanRepository.findById(openLoan.getId())).thenReturn(Optional.of(openLoan));
        when(installmentRepository.findByLoanIdAndStatusOrderByDueDateAsc(openLoan.getId(), InstallmentStatus.PENDING))
                .thenReturn(List.of(inst1, inst2));
        when(repaymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(loanRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(installmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Pay exactly first installment
        validRequest.setAmount(BigDecimal.valueOf(3000));
        repaymentService.processRepayment(validRequest);

        assertThat(inst1.getStatus()).isEqualTo(InstallmentStatus.PAID);
        assertThat(inst2.getStatus()).isEqualTo(InstallmentStatus.PENDING); // untouched
    }

    @Test
    void processRepayment_publishesRepaymentReceivedEvent() {
        when(repaymentRepository.existsByReference(any())).thenReturn(false);
        when(loanRepository.findById(openLoan.getId())).thenReturn(Optional.of(openLoan));
        when(installmentRepository.findByLoanIdAndStatusOrderByDueDateAsc(any(), any())).thenReturn(List.of());
        when(repaymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(loanRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        repaymentService.processRepayment(validRequest);

        ArgumentCaptor<RepaymentReceivedEvent> captor = ArgumentCaptor.forClass(RepaymentReceivedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().amountPaid()).isEqualByComparingTo("3000.00");
    }
}
```

- [ ] **Step 8: Run repayment tests**

```bash
mvn test -Dtest="RepaymentServiceTest" -q
```

Expected: `Tests run: 6, Failures: 0, Errors: 0`

- [ ] **Step 9: Commit**

```bash
git add src/main/java/com/tezza/lending/repayment/ \
        src/test/java/com/tezza/lending/repayment/
git commit -m "feat: add repayment module — processing, balance deduction, installment allocation FIFO"
```
