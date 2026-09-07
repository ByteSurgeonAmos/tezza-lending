package com.tezza.lending.product;

import com.tezza.lending.product.api.dto.FeeRequest;
import com.tezza.lending.product.api.dto.ProductRequest;
import com.tezza.lending.product.api.dto.ProductResponse;
import com.tezza.lending.product.internal.entity.LoanProduct;
import com.tezza.lending.product.internal.entity.enums.CalculationType;
import com.tezza.lending.product.internal.entity.enums.FeeType;
import com.tezza.lending.product.internal.entity.enums.TenureType;
import com.tezza.lending.product.internal.repository.LoanProductRepository;
import com.tezza.lending.product.internal.repository.ProductFeeRepository;
import com.tezza.lending.product.internal.service.ProductServiceImpl;
import com.tezza.lending.shared.exception.BusinessException;
import com.tezza.lending.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock LoanProductRepository productRepository;
    @Mock ProductFeeRepository feeRepository;

    ProductServiceImpl productService;
    ProductRequest validRequest;

    @BeforeEach
    void setUp() {
        productService = new ProductServiceImpl(productRepository, feeRepository);

        validRequest = new ProductRequest();
        validRequest.setName("Test Loan");
        validRequest.setTenureValue(30);
        validRequest.setTenureType(TenureType.DAYS);
        validRequest.setMinAmount(BigDecimal.valueOf(1000));
        validRequest.setMaxAmount(BigDecimal.valueOf(50000));
    }

    @Test
    void createProduct_setsActiveTrue() {
        when(productRepository.existsByNameIgnoreCase("Test Loan")).thenReturn(false);
        when(productRepository.save(any())).thenAnswer(inv -> {
            LoanProduct p = inv.getArgument(0);
            p.setFees(new ArrayList<>());
            return p;
        });

        ProductResponse result = productService.createProduct(validRequest);

        assertThat(result.isActive()).isTrue();
        verify(productRepository).save(any(LoanProduct.class));
    }

    @Test
    void createProduct_duplicateName_throwsBusinessException() {
        when(productRepository.existsByNameIgnoreCase("Test Loan")).thenReturn(true);

        assertThatThrownBy(() -> productService.createProduct(validRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void createProduct_maxAmountLessThanMin_throwsBusinessException() {
        validRequest.setMaxAmount(BigDecimal.valueOf(500));
        when(productRepository.existsByNameIgnoreCase(any())).thenReturn(false);

        assertThatThrownBy(() -> productService.createProduct(validRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("maxAmount");
    }

    @Test
    void deactivateProduct_setsActiveFalse() {
        LoanProduct product = new LoanProduct();
        product.setId(UUID.randomUUID());
        product.setActive(true);
        product.setFees(new ArrayList<>());
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(productRepository.save(any())).thenReturn(product);

        productService.deactivateProduct(product.getId());

        assertThat(product.isActive()).isFalse();
    }

    @Test
    void getProduct_notFound_throwsResourceNotFoundException() {
        UUID id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProduct(id))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void addFee_linksToProduct() {
        LoanProduct product = new LoanProduct();
        product.setId(UUID.randomUUID());
        product.setActive(true);
        product.setFees(new ArrayList<>());
        when(productRepository.findById(product.getId())).thenReturn(Optional.of(product));
        when(productRepository.save(any())).thenReturn(product);

        FeeRequest feeRequest = new FeeRequest();
        feeRequest.setFeeType(FeeType.SERVICE_FEE);
        feeRequest.setCalculationType(CalculationType.PERCENTAGE);
        feeRequest.setAmount(BigDecimal.valueOf(5));
        feeRequest.setDaysAfterDue(0);

        productService.addFee(product.getId(), feeRequest);

        assertThat(product.getFees()).hasSize(1);
        assertThat(product.getFees().get(0).getFeeType()).isEqualTo(FeeType.SERVICE_FEE);
    }

    @Test
    void listProducts_returnsPaginated() {
        LoanProduct product = new LoanProduct();
        product.setId(UUID.randomUUID());
        product.setActive(true);
        product.setFees(new ArrayList<>());
        when(productRepository.findByActive(true, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(product)));

        var page = productService.listProducts(true, PageRequest.of(0, 20));

        assertThat(page.getContent()).hasSize(1);
    }
}
