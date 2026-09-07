package com.tezza.lending.product.internal.repository;

import com.tezza.lending.product.internal.entity.LoanProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LoanProductRepository extends JpaRepository<LoanProduct, UUID> {
    Page<LoanProduct> findByActive(boolean active, Pageable pageable);
    boolean existsByNameIgnoreCase(String name);
}
