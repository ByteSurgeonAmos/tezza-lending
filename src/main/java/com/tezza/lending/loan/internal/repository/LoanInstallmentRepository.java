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
    int countByLoanIdAndStatus(UUID loanId, InstallmentStatus status);
}
