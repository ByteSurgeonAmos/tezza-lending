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

    // RETURNS OPEN/OVERDUE LOANS; SWEEP JOB CHECKS PRODUCT FOR DAILY_FEE PRESENCE
    @Query("SELECT l FROM Loan l WHERE l.status IN ('OPEN', 'OVERDUE')")
    List<Loan> findActiveLoans();

    @Query("SELECT l FROM Loan l WHERE l.dueDate = :targetDate AND l.status = 'OPEN'")
    List<Loan> findLoansDueOn(@Param("targetDate") LocalDate targetDate);
}
