package com.tezza.lending.loan.api.dto;

import com.tezza.lending.loan.internal.entity.LoanInstallment;
import com.tezza.lending.loan.internal.entity.enums.InstallmentStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class InstallmentResponse {
    private UUID id;
    private int installmentNumber;
    private BigDecimal principalAmount;
    private BigDecimal feeAmount;
    private BigDecimal totalAmount;
    private BigDecimal outstandingAmount;
    private LocalDate dueDate;
    private LocalDateTime paidAt;
    private InstallmentStatus status;

    public static InstallmentResponse from(LoanInstallment i) {
        InstallmentResponse r = new InstallmentResponse();
        r.id = i.getId();
        r.installmentNumber = i.getInstallmentNumber();
        r.principalAmount = i.getPrincipalAmount();
        r.feeAmount = i.getFeeAmount();
        r.totalAmount = i.getTotalAmount();
        r.outstandingAmount = i.getOutstandingAmount();
        r.dueDate = i.getDueDate();
        r.paidAt = i.getPaidAt();
        r.status = i.getStatus();
        return r;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public int getInstallmentNumber() { return installmentNumber; }
    public void setInstallmentNumber(int installmentNumber) { this.installmentNumber = installmentNumber; }
    public BigDecimal getPrincipalAmount() { return principalAmount; }
    public void setPrincipalAmount(BigDecimal principalAmount) { this.principalAmount = principalAmount; }
    public BigDecimal getFeeAmount() { return feeAmount; }
    public void setFeeAmount(BigDecimal feeAmount) { this.feeAmount = feeAmount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public BigDecimal getOutstandingAmount() { return outstandingAmount; }
    public void setOutstandingAmount(BigDecimal outstandingAmount) { this.outstandingAmount = outstandingAmount; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }
    public InstallmentStatus getStatus() { return status; }
    public void setStatus(InstallmentStatus status) { this.status = status; }
}
