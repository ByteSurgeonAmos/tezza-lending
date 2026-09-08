package com.tezza.lending.notification.internal.kafka;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public class LoanEventMessage {
    private String eventType;
    private UUID loanId;
    private UUID customerId;
    private String customerEmail;
    private String customerPhone;
    private String customerName;
    private String loanNumber;
    private BigDecimal amount;
    private BigDecimal outstandingBalance;
    private LocalDate dueDate;

    public LoanEventMessage() {}

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public UUID getLoanId() { return loanId; }
    public void setLoanId(UUID loanId) { this.loanId = loanId; }
    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }
    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getLoanNumber() { return loanNumber; }
    public void setLoanNumber(String loanNumber) { this.loanNumber = loanNumber; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getOutstandingBalance() { return outstandingBalance; }
    public void setOutstandingBalance(BigDecimal outstandingBalance) { this.outstandingBalance = outstandingBalance; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private final LoanEventMessage msg = new LoanEventMessage();
        public Builder eventType(String v) { msg.eventType = v; return this; }
        public Builder loanId(UUID v) { msg.loanId = v; return this; }
        public Builder customerId(UUID v) { msg.customerId = v; return this; }
        public Builder customerEmail(String v) { msg.customerEmail = v; return this; }
        public Builder customerPhone(String v) { msg.customerPhone = v; return this; }
        public Builder customerName(String v) { msg.customerName = v; return this; }
        public Builder loanNumber(String v) { msg.loanNumber = v; return this; }
        public Builder amount(BigDecimal v) { msg.amount = v; return this; }
        public Builder outstandingBalance(BigDecimal v) { msg.outstandingBalance = v; return this; }
        public Builder dueDate(LocalDate v) { msg.dueDate = v; return this; }
        public LoanEventMessage build() { return msg; }
    }
}
