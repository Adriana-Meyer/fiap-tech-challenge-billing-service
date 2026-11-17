package com.fiap.workshop.billing.domain.model.payment;

import com.fiap.workshop.billing.domain.exception.InvalidStateException;
import com.fiap.workshop.billing.domain.gateway.ChargeResult;
import com.fiap.workshop.billing.domain.model.shared.Money;

import java.time.Instant;
import java.util.UUID;

/** The payment attempt of one service order. There is at most one per order. */
public class Payment {

    private final UUID id;
    private final UUID serviceOrderId;
    private final UUID budgetId;
    private final Money amount;
    private final String paymentMethod;
    private final String externalReference;
    private final String declineReason;
    private final Instant createdAt;
    private PaymentStatus status;
    private Instant updatedAt;

    public Payment(UUID id, UUID serviceOrderId, UUID budgetId, Money amount, String paymentMethod,
                   PaymentStatus status, String externalReference, String declineReason,
                   Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.serviceOrderId = serviceOrderId;
        this.budgetId = budgetId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.externalReference = externalReference;
        this.declineReason = declineReason;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Payment fromCharge(UUID serviceOrderId, UUID budgetId, Money amount,
                                     String paymentMethod, ChargeResult result) {
        Instant now = Instant.now();
        return new Payment(UUID.randomUUID(), serviceOrderId, budgetId, amount, paymentMethod,
                result.approved() ? PaymentStatus.APPROVED : PaymentStatus.DECLINED,
                result.externalReference(), result.declineReason(), now, now);
    }

    public void refund() {
        if (status != PaymentStatus.APPROVED) {
            throw new InvalidStateException(
                    "Payment of service order " + serviceOrderId + " cannot be refunded in status " + status);
        }
        this.status = PaymentStatus.REFUNDED;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getServiceOrderId() { return serviceOrderId; }
    public UUID getBudgetId() { return budgetId; }
    public Money getAmount() { return amount; }
    public String getPaymentMethod() { return paymentMethod; }
    public PaymentStatus getStatus() { return status; }
    public String getExternalReference() { return externalReference; }
    public String getDeclineReason() { return declineReason; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
