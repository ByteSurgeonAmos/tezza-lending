package com.tezza.lending.customer.internal.repository;

import com.tezza.lending.customer.internal.entity.CustomerLoanLimit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CustomerLoanLimitRepository extends JpaRepository<CustomerLoanLimit, UUID> {
    Optional<CustomerLoanLimit> findByCustomerId(UUID customerId);
}
