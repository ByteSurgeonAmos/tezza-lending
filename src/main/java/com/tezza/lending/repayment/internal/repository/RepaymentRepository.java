package com.tezza.lending.repayment.internal.repository;

import com.tezza.lending.repayment.internal.entity.Repayment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RepaymentRepository extends JpaRepository<Repayment, UUID> {
    List<Repayment> findByLoanIdOrderByProcessedAtDesc(UUID loanId);
    boolean existsByReference(String reference);
}
