package com.tezza.lending.loan.api.dto;

import com.tezza.lending.loan.internal.entity.Loan;
import com.tezza.lending.loan.internal.entity.enums.LoanStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class LoanSummaryResponse {

    private UUID loanId;
    private String loanNumber;
    private LoanStatus status;
    private BigDecimal principalAmount;
    private BigDecimal outstandingBalance;
    private BigDecimal totalPaid;
    private LocalDate dueDate;
    private int overdueInstallmentCount;

    public static LoanSummaryResponse from(Loan loan, int overdueInstallmentCount) {
        LoanSummaryResponse r = new LoanSummaryResponse();
        r.loanId = loan.getId();
        r.loanNumber = loan.getLoanNumber();
        r.status = loan.getStatus();
        r.principalAmount = loan.getPrincipalAmount();
        r.outstandingBalance = loan.getOutstandingBalance();
        r.totalPaid = loan.getPrincipalAmount().subtract(loan.getOutstandingBalance()).max(BigDecimal.ZERO);
        r.dueDate = loan.getDueDate();
        r.overdueInstallmentCount = overdueInstallmentCount;
        return r;
    }

    public UUID getLoanId() { return loanId; }
    public String getLoanNumber() { return loanNumber; }
    public LoanStatus getStatus() { return status; }
    public BigDecimal getPrincipalAmount() { return principalAmount; }
    public BigDecimal getOutstandingBalance() { return outstandingBalance; }
    public BigDecimal getTotalPaid() { return totalPaid; }
    public LocalDate getDueDate() { return dueDate; }
    public int getOverdueInstallmentCount() { return overdueInstallmentCount; }
}
