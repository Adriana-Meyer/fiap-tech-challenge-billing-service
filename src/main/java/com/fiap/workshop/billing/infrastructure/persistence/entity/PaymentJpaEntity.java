package com.fiap.workshop.billing.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class PaymentJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "service_order_id", nullable = false)
    private UUID serviceOrderId;

    @Column(name = "budget_id", nullable = false)
    private UUID budgetId;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "payment_method", length = 50, nullable = false)
    private String paymentMethod;

    @Column(name = "status", length = 20, nullable = false)
    private String status;

    @Column(name = "external_reference", length = 100)
    private String externalReference;

    @Column(name = "decline_reason")
    private String declineReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PaymentJpaEntity() {}

    public PaymentJpaEntity(UUID id, UUID serviceOrderId, UUID budgetId, BigDecimal amount,
                            String paymentMethod, String status, String externalReference,
                            String declineReason, Instant createdAt, Instant updatedAt) {
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

    public UUID getId() { return id; }
    public UUID getServiceOrderId() { return serviceOrderId; }
    public UUID getBudgetId() { return budgetId; }
    public BigDecimal getAmount() { return amount; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getStatus() { return status; }
    public String getExternalReference() { return externalReference; }
    public String getDeclineReason() { return declineReason; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
