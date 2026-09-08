package com.tezza.lending.loan.internal.scheduler;

import com.tezza.lending.loan.api.event.LoanOverdueEvent;
import com.tezza.lending.loan.internal.entity.Loan;
import com.tezza.lending.loan.internal.entity.LoanInstallment;
import com.tezza.lending.loan.internal.entity.enums.InstallmentStatus;
import com.tezza.lending.loan.internal.entity.enums.LoanStatus;
import com.tezza.lending.loan.internal.repository.LoanRepository;
import com.tezza.lending.loan.internal.service.FeeCalculatorService;
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

    public OverdueSweepJob(LoanRepository loanRepository, FeeCalculatorService feeCalculatorService,
                           ApplicationEventPublisher eventPublisher) {
        this.loanRepository = loanRepository;
        this.feeCalculatorService = feeCalculatorService;
        this.eventPublisher = eventPublisher;
    }

    @Scheduled(cron = "0 5 0 * * *")
    @Transactional
    public void runOverdueSweep() {
        LocalDate today = LocalDate.now();
        List<Loan> overdueLoans = loanRepository.findOverdueLoans(today);
        log.info("OverdueSweepJob: processing {} overdue loans", overdueLoans.size());

        for (Loan loan : overdueLoans) {
            int daysOverdue = (int) (today.toEpochDay() - loan.getDueDate().toEpochDay());

            var lateFee = feeCalculatorService.calculateLateFees(
                    loan.getProductId(), loan.getOutstandingBalance(), daysOverdue);
            if (lateFee.signum() > 0) {
                loan.setOutstandingBalance(loan.getOutstandingBalance().add(lateFee));
                log.info("Applied late fee {} to loan {}", lateFee, loan.getLoanNumber());
            }

            for (LoanInstallment installment : loan.getInstallments()) {
                if (installment.getStatus() == InstallmentStatus.PENDING &&
                    installment.getDueDate().isBefore(today)) {
                    installment.setStatus(InstallmentStatus.OVERDUE);
                }
            }

            loan.setStatus(LoanStatus.OVERDUE);
            loanRepository.save(loan);

            eventPublisher.publishEvent(new LoanOverdueEvent(
                    loan.getId(), loan.getCustomerId(),
                    loan.getLoanNumber(), loan.getOutstandingBalance()));
        }
        log.info("OverdueSweepJob: completed, {} loans marked overdue", overdueLoans.size());
    }
}
