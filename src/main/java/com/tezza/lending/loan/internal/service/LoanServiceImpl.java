package com.tezza.lending.loan.internal.service;

import com.tezza.lending.customer.internal.entity.CustomerLoanLimit;
import com.tezza.lending.customer.internal.repository.CustomerLoanLimitRepository;
import com.tezza.lending.customer.internal.repository.CustomerRepository;
import com.tezza.lending.loan.api.LoanService;
import com.tezza.lending.loan.api.dto.*;
import com.tezza.lending.loan.api.event.LoanCreatedEvent;
import com.tezza.lending.loan.internal.entity.Loan;
import com.tezza.lending.loan.internal.entity.LoanInstallment;
import com.tezza.lending.loan.internal.entity.enums.BillingCycleType;
import com.tezza.lending.loan.internal.entity.enums.LoanStatus;
import com.tezza.lending.loan.internal.entity.enums.LoanType;
import com.tezza.lending.loan.internal.repository.LoanInstallmentRepository;
import com.tezza.lending.loan.internal.repository.LoanRepository;
import com.tezza.lending.product.internal.entity.LoanProduct;
import com.tezza.lending.product.internal.repository.LoanProductRepository;
import com.tezza.lending.shared.exception.BusinessException;
import com.tezza.lending.shared.exception.ResourceNotFoundException;
import com.tezza.lending.shared.util.LoanNumberGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class LoanServiceImpl implements LoanService {

    private static final Logger log = LoggerFactory.getLogger(LoanServiceImpl.class);

    private final LoanRepository loanRepository;
    private final LoanInstallmentRepository installmentRepository;
    private final LoanProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final CustomerLoanLimitRepository loanLimitRepository;
    private final FeeCalculatorService feeCalculatorService;
    private final InstallmentGeneratorService installmentGeneratorService;
    private final ApplicationEventPublisher eventPublisher;

    public LoanServiceImpl(
            LoanRepository loanRepository,
            LoanInstallmentRepository installmentRepository,
            LoanProductRepository productRepository,
            CustomerRepository customerRepository,
            CustomerLoanLimitRepository loanLimitRepository,
            FeeCalculatorService feeCalculatorService,
            InstallmentGeneratorService installmentGeneratorService,
            ApplicationEventPublisher eventPublisher) {
        this.loanRepository = loanRepository;
        this.installmentRepository = installmentRepository;
        this.productRepository = productRepository;
        this.customerRepository = customerRepository;
        this.loanLimitRepository = loanLimitRepository;
        this.feeCalculatorService = feeCalculatorService;
        this.installmentGeneratorService = installmentGeneratorService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public LoanResponse disburseLoan(LoanRequest request) {
        customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer", request.getCustomerId().toString()));

        LoanProduct product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("LoanProduct", request.getProductId().toString()));
        if (!product.isActive()) {
            throw new BusinessException("Loan product is not active");
        }

        if (request.getAmount().compareTo(product.getMinAmount()) < 0 ||
            request.getAmount().compareTo(product.getMaxAmount()) > 0) {
            throw new BusinessException(String.format(
                "Loan amount must be between %s and %s", product.getMinAmount(), product.getMaxAmount()));
        }

        CustomerLoanLimit limit = loanLimitRepository.findByCustomerId(request.getCustomerId())
                .orElseThrow(() -> new BusinessException("No loan limit configured for customer"));
        if (request.getAmount().compareTo(limit.getCurrentLimit()) > 0) {
            throw new BusinessException("Requested amount exceeds customer loan limit of " + limit.getCurrentLimit());
        }

        if (request.getLoanType() == LoanType.INSTALLMENT &&
            (request.getNumInstallments() == null || request.getNumInstallments() < 1)) {
            throw new BusinessException("numInstallments required for INSTALLMENT loan type");
        }

        LocalDate disbursementDate = LocalDate.now();
        LocalDate dueDate = calculateDueDate(disbursementDate, product, request);

        Loan loan = new Loan();
        loan.setLoanNumber(LoanNumberGenerator.generate());
        loan.setCustomerId(request.getCustomerId());
        loan.setProductId(request.getProductId());
        loan.setPrincipalAmount(request.getAmount());
        loan.setDisbursedAmount(request.getAmount());
        loan.setOutstandingBalance(request.getAmount());
        loan.setStatus(LoanStatus.OPEN);
        loan.setLoanType(request.getLoanType());
        loan.setBillingCycleType(request.getBillingCycleType());
        loan.setConsolidatedDueDate(request.getConsolidatedDueDate());
        loan.setDueDate(dueDate);
        loan.setDisbursedAt(LocalDateTime.now());

        if (request.getLoanType() == LoanType.INSTALLMENT) {
            BigDecimal serviceFeeTotal = feeCalculatorService.calculateOriginationFees(
                    product.getId(), request.getAmount());
            BigDecimal feePerInstallment = serviceFeeTotal.divide(
                    BigDecimal.valueOf(request.getNumInstallments()), 2, RoundingMode.HALF_UP);
            List<LoanInstallment> installments = installmentGeneratorService.generate(
                    loan, request.getNumInstallments(), product.getTenureType(),
                    product.getTenureValue(), disbursementDate, feePerInstallment);
            loan.getInstallments().addAll(installments);
        }

        Loan saved = loanRepository.save(loan);
        log.info("Loan disbursed: {} for customer {}", saved.getLoanNumber(), request.getCustomerId());

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
    public LoanSummaryResponse getLoanSummary(UUID id) {
        Loan loan = findLoan(id);
        int overdueCount = installmentRepository.countByLoanIdAndStatus(id, com.tezza.lending.loan.internal.entity.enums.InstallmentStatus.OVERDUE);
        return LoanSummaryResponse.from(loan, overdueCount);
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
        if (request.getBillingCycleType() == BillingCycleType.CONSOLIDATED
            && request.getConsolidatedDueDate() != null) {
            return request.getConsolidatedDueDate();
        }
        return switch (product.getTenureType()) {
            case DAYS -> disbursementDate.plusDays(product.getTenureValue());
            case MONTHS -> disbursementDate.plusMonths(product.getTenureValue());
        };
    }
}
