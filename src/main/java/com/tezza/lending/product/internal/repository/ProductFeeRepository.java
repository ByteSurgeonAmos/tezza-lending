package com.tezza.lending.product.internal.repository;

import com.tezza.lending.product.internal.entity.ProductFee;
import com.tezza.lending.product.internal.entity.enums.FeeType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductFeeRepository extends JpaRepository<ProductFee, UUID> {
    List<ProductFee> findByProductIdAndActive(UUID productId, boolean active);
    List<ProductFee> findByProductIdAndFeeType(UUID productId, FeeType feeType);
}
