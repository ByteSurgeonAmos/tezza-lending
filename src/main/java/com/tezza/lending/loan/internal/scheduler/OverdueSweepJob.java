package com.tezza.lending.loan.internal.scheduler;

import com.tezza.lending.loan.api.event.LoanOverdueEvent;
import com.tezza.lending.loan.internal.entity.Loan;
import com.tezza.lending.loan.internal.entity.enums.LoanStatus;
import com.tezza.lending.loan.internal.repository.LoanRepository;
import com.tezza.lending.loan.internal.service.FeeCalculatorService;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
public class OverdueSweepJob {

    private static final Logger log = LoggerFactory.getLogger(OverdueSweepJob.class);
    private final LoanRepository loanRepository;
    private final FeeCalculatorService feeCalculatorService;
    private final ApplicationEventPublisher eventPublisher;
    private final EntityManager entityManager;

    public OverdueSweepJob(LoanRepository loanRepository,
                           FeeCalculatorService feeCalculatorService,
                           ApplicationEventPublisher eventPublisher,
                           EntityManager entityManager) {
        this.loanRepository = loanRepository;
        this.feeCalculatorService = feeCalculatorService;
        this.eventPublisher = eventPublisher;
        this.entityManager = entityManager;
    }

    @Scheduled(cron = "0 5 0 * * *")
    @Transactional
    public void runOverdueSweep() {
        LocalDate today = LocalDate.now();

        // FETCH loans that will become overdue (still OPEN, past due date)
        // — fetch BEFORE the sweep so we can calculate product-specific fees
        List<Loan> loansToMark = loanRepository.findOverdueLoans(today);
        log.info("OverdueSweepJob: {} loans to mark overdue", loansToMark.size());

        // APPLY product-specific late fees BEFORE status transition
        for (Loan loan : loansToMark) {
            int daysOverdue = (int) (today.toEpochDay() - loan.getDueDate().toEpochDay());
            var lateFee = feeCalculatorService.calculateLateFees(
                    loan.getProductId(), loan.getOutstandingBalance(), daysOverdue);

            if (lateFee.signum() > 0) {
                // LATE FEE via stored procedure
                entityManager.createNativeQuery("CALL proc_apply_late_fee(:loanId, :fee)")
                        .setParameter("loanId", loan.getId())
                        .setParameter("fee", lateFee)
                        .executeUpdate();
                log.info("Applied late fee {} to loan {} via SP", lateFee, loan.getLoanNumber());
            }
        }

        // OVERDUE STATUS TRANSITION + INSTALLMENT MARKING via stored procedure
        entityManager.createNativeQuery("CALL proc_run_overdue_sweep(NULL)")
                .executeUpdate();

        // RELOAD and publish events
        entityManager.clear();
        List<Loan> overdueLoans = loanRepository.findByStatus(LoanStatus.OVERDUE,
                org.springframework.data.domain.Pageable.unpaged()).getContent();
        // ONLY publish for loans that were just transitioned (were in loansToMark)
        var justMarked = loansToMark.stream().map(Loan::getId).collect(java.util.stream.Collectors.toSet());
        overdueLoans.stream()
                .filter(l -> justMarked.contains(l.getId()))
                .forEach(loan -> eventPublisher.publishEvent(new LoanOverdueEvent(
                        loan.getId(), loan.getCustomerId(),
                        loan.getLoanNumber(), loan.getOutstandingBalance())));

        log.info("OverdueSweepJob: completed, {} loans marked overdue", loansToMark.size());
    }
}
