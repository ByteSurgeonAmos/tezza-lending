package com.tezza.lending.loan.api.event;

import java.math.BigDecimal;
import java.util.UUID;

public record RepaymentReceivedEvent(
    UUID loanId,
    UUID customerId,
    String loanNumber,
    BigDecimal amountPaid,
    BigDecimal remainingBalance
) {}
