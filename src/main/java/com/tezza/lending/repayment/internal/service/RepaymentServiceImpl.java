package com.tezza.lending.repayment.internal.service;

import com.tezza.lending.loan.api.LoanService;
import com.tezza.lending.loan.api.dto.LoanResponse;
import com.tezza.lending.loan.api.RepaymentReceivedEvent;
import com.tezza.lending.repayment.api.RepaymentService;
import com.tezza.lending.repayment.api.dto.RepaymentRequest;
import com.tezza.lending.repayment.api.dto.RepaymentResponse;
import com.tezza.lending.repayment.internal.entity.Repayment;
import com.tezza.lending.repayment.internal.repository.RepaymentRepository;
import com.tezza.lending.shared.exception.BusinessException;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class RepaymentServiceImpl implements RepaymentService {

    private static final Logger log = LoggerFactory.getLogger(RepaymentServiceImpl.class);
    private final RepaymentRepository repaymentRepository;
    private final LoanService loanService;
    private final EntityManager entityManager;
    private final ApplicationEventPublisher eventPublisher;

    public RepaymentServiceImpl(RepaymentRepository repaymentRepository,
                                LoanService loanService,
                                EntityManager entityManager,
                                ApplicationEventPublisher eventPublisher) {
        this.repaymentRepository = repaymentRepository;
        this.loanService = loanService;
        this.entityManager = entityManager;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public RepaymentResponse processRepayment(RepaymentRequest request) {
        if (repaymentRepository.existsByReference(request.getReference())) {
            throw new BusinessException("Duplicate repayment reference: " + request.getReference());
        }

        // VALIDATES LOAN IS OPEN/OVERDUE — throws BusinessException otherwise
        LoanResponse loan = loanService.getRepayableLoan(request.getLoanId());

        BigDecimal paymentAmount = request.getAmount();
        BigDecimal newBalance = loan.getOutstandingBalance().subtract(paymentAmount).max(BigDecimal.ZERO);

        // DEDUCT BALANCE — update outstanding balance directly
        entityManager.createNativeQuery(
                "UPDATE LOANS SET OUTSTANDING_BALANCE = :balance, UPDATED_AT = NOW() WHERE ID = :id")
                .setParameter("balance", newBalance)
                .setParameter("id", loan.getId())
                .executeUpdate();

        // FIFO INSTALLMENT ALLOCATION via stored procedure
        entityManager.createNativeQuery("CALL proc_allocate_repayment(:loanId, :amount)")
                .setParameter("loanId", loan.getId())
                .setParameter("amount", paymentAmount)
                .executeUpdate();

        // CLOSE LOAN IF FULLY PAID via stored procedure
        entityManager.createNativeQuery("CALL proc_close_loan_if_paid(:loanId)")
                .setParameter("loanId", loan.getId())
                .executeUpdate();

        log.info("Repayment processed for loan {}: amount={}, new balance={}",
                loan.getLoanNumber(), paymentAmount, newBalance);

        Repayment repayment = new Repayment();
        repayment.setLoanId(request.getLoanId());
        repayment.setAmount(paymentAmount);
        repayment.setReference(request.getReference());
        repayment.setChannel(request.getChannel());
        repayment.setProcessedAt(LocalDateTime.now());
        repaymentRepository.save(repayment);

        eventPublisher.publishEvent(new RepaymentReceivedEvent(
                loan.getId(), loan.getCustomerId(),
                loan.getLoanNumber(), paymentAmount, newBalance));

        return RepaymentResponse.from(repayment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RepaymentResponse> getLoanRepayments(UUID loanId) {
        return repaymentRepository.findByLoanIdOrderByProcessedAtDesc(loanId)
                .stream().map(RepaymentResponse::from).toList();
    }
}
