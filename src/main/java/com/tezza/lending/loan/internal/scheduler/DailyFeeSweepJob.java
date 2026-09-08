package com.tezza.lending.loan.internal.scheduler;

import com.tezza.lending.loan.internal.entity.Loan;
import com.tezza.lending.loan.internal.repository.LoanRepository;
import com.tezza.lending.loan.internal.service.FeeCalculatorService;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Component
public class DailyFeeSweepJob {

    private static final Logger log = LoggerFactory.getLogger(DailyFeeSweepJob.class);
    private final LoanRepository loanRepository;
    private final FeeCalculatorService feeCalculatorService;
    private final EntityManager entityManager;

    public DailyFeeSweepJob(LoanRepository loanRepository,
                            FeeCalculatorService feeCalculatorService,
                            EntityManager entityManager) {
        this.loanRepository = loanRepository;
        this.feeCalculatorService = feeCalculatorService;
        this.entityManager = entityManager;
    }

    @Scheduled(cron = "0 10 0 * * *")
    @Transactional
    public void runDailyFeeSweep() {
        List<Loan> loans = loanRepository.findLoansWithDailyFee();
        log.info("DailyFeeSweepJob: accruing daily fees on {} loans", loans.size());

        for (Loan loan : loans) {
            BigDecimal dailyFee = feeCalculatorService.calculateDailyFee(
                    loan.getProductId(), loan.getOutstandingBalance());
            if (dailyFee.signum() > 0) {
                // DAILY FEE ACCRUAL via stored procedure
                entityManager.createNativeQuery("CALL proc_apply_daily_fee(:loanId, :fee)")
                        .setParameter("loanId", loan.getId())
                        .setParameter("fee", dailyFee)
                        .executeUpdate();
                log.debug("Accrued daily fee {} on loan {} via SP", dailyFee, loan.getLoanNumber());
            }
        }
    }
}
