package com.tezza.lending.customer.internal.repository;

import com.tezza.lending.customer.internal.entity.Customer;
import com.tezza.lending.customer.internal.entity.enums.CustomerStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    Optional<Customer> findByEmail(String email);
    Optional<Customer> findByNationalId(String nationalId);
    boolean existsByEmail(String email);
    boolean existsByNationalId(String nationalId);
    Page<Customer> findByStatus(CustomerStatus status, Pageable pageable);
}
