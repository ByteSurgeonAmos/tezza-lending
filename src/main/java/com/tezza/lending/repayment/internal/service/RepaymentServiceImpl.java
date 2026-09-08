package com.tezza.lending.repayment.internal.service;

import com.tezza.lending.loan.api.event.RepaymentReceivedEvent;
import com.tezza.lending.loan.internal.entity.Loan;
import com.tezza.lending.loan.internal.entity.LoanInstallment;
import com.tezza.lending.loan.internal.entity.enums.InstallmentStatus;
import com.tezza.lending.loan.internal.entity.enums.LoanStatus;
import com.tezza.lending.loan.internal.repository.LoanInstallmentRepository;
import com.tezza.lending.loan.internal.repository.LoanRepository;
import com.tezza.lending.repayment.api.RepaymentService;
import com.tezza.lending.repayment.api.dto.RepaymentRequest;
import com.tezza.lending.repayment.api.dto.RepaymentResponse;
import com.tezza.lending.repayment.internal.entity.Repayment;
import com.tezza.lending.repayment.internal.repository.RepaymentRepository;
import com.tezza.lending.shared.exception.BusinessException;
import com.tezza.lending.shared.exception.ResourceNotFoundException;
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
    private final LoanRepository loanRepository;
    private final LoanInstallmentRepository installmentRepository;
    private final ApplicationEventPublisher eventPublisher;

    public RepaymentServiceImpl(RepaymentRepository repaymentRepository,
                                LoanRepository loanRepository,
                                LoanInstallmentRepository installmentRepository,
                                ApplicationEventPublisher eventPublisher) {
        this.repaymentRepository = repaymentRepository;
        this.loanRepository = loanRepository;
        this.installmentRepository = installmentRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public RepaymentResponse processRepayment(RepaymentRequest request) {
        if (repaymentRepository.existsByReference(request.getReference())) {
            throw new BusinessException("Duplicate repayment reference: " + request.getReference());
        }

        Loan loan = loanRepository.findById(request.getLoanId())
                .orElseThrow(() -> new ResourceNotFoundException("Loan", request.getLoanId().toString()));

        if (loan.getStatus() != LoanStatus.OPEN && loan.getStatus() != LoanStatus.OVERDUE) {
            throw new BusinessException("Cannot process repayment for loan in status: " + loan.getStatus());
        }

        BigDecimal paymentAmount = request.getAmount();

        BigDecimal newBalance = loan.getOutstandingBalance().subtract(paymentAmount);
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            newBalance = BigDecimal.ZERO;
        }
        loan.setOutstandingBalance(newBalance);

        allocateToInstallments(loan.getId(), paymentAmount);

        if (newBalance.compareTo(BigDecimal.ZERO) == 0) {
            loan.setStatus(LoanStatus.CLOSED);
            loan.setClosedAt(LocalDateTime.now());
            log.info("Loan {} fully repaid and closed", loan.getLoanNumber());
        } else if (loan.getStatus() == LoanStatus.OVERDUE && newBalance.signum() > 0) {
            loan.setStatus(LoanStatus.OPEN);
        }

        loanRepository.save(loan);

        Repayment repayment = new Repayment();
        repayment.setLoanId(request.getLoanId());
        repayment.setAmount(paymentAmount);
        repayment.setReference(request.getReference());
        repayment.setChannel(request.getChannel());
        repayment.setProcessedAt(LocalDateTime.now());
        repaymentRepository.save(repayment);

        eventPublisher.publishEvent(new RepaymentReceivedEvent(
                loan.getId(), loan.getCustomerId(),
                loan.getLoanNumber(), paymentAmount, loan.getOutstandingBalance()));

        return RepaymentResponse.from(repayment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RepaymentResponse> getLoanRepayments(UUID loanId) {
        return repaymentRepository.findByLoanIdOrderByProcessedAtDesc(loanId)
                .stream().map(RepaymentResponse::from).toList();
    }

    private void allocateToInstallments(UUID loanId, BigDecimal paymentAmount) {
        List<LoanInstallment> pendingInstallments = installmentRepository
                .findByLoanIdAndStatusOrderByDueDateAsc(loanId, InstallmentStatus.PENDING);

        BigDecimal remaining = paymentAmount;
        for (LoanInstallment installment : pendingInstallments) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;

            if (remaining.compareTo(installment.getOutstandingAmount()) >= 0) {
                remaining = remaining.subtract(installment.getOutstandingAmount());
                installment.setOutstandingAmount(BigDecimal.ZERO);
                installment.setStatus(InstallmentStatus.PAID);
                installment.setPaidAt(LocalDateTime.now());
            } else {
                installment.setOutstandingAmount(installment.getOutstandingAmount().subtract(remaining));
                remaining = BigDecimal.ZERO;
            }
            installmentRepository.save(installment);
        }
    }
}
