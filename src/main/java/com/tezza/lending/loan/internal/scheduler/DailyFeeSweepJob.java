package com.tezza.lending.loan.internal.scheduler;

import com.tezza.lending.loan.internal.entity.Loan;
import com.tezza.lending.loan.internal.repository.LoanRepository;
import com.tezza.lending.loan.internal.service.FeeCalculatorService;
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

    public DailyFeeSweepJob(LoanRepository loanRepository, FeeCalculatorService feeCalculatorService) {
        this.loanRepository = loanRepository;
        this.feeCalculatorService = feeCalculatorService;
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
                loan.setOutstandingBalance(loan.getOutstandingBalance().add(dailyFee));
                loanRepository.save(loan);
                log.debug("Accrued daily fee {} on loan {}", dailyFee, loan.getLoanNumber());
            }
        }
    }
}
