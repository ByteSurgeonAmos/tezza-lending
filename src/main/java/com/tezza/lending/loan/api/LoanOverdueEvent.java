package com.tezza.lending.loan.api;

import java.math.BigDecimal;
import java.util.UUID;

public record LoanOverdueEvent(
    UUID loanId,
    UUID customerId,
    String loanNumber,
    BigDecimal outstandingBalance
) {}
