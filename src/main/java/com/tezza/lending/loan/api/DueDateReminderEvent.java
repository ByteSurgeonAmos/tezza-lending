package com.tezza.lending.loan.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record DueDateReminderEvent(
    UUID loanId,
    UUID customerId,
    String loanNumber,
    BigDecimal outstandingBalance,
    LocalDate dueDate
) {}
