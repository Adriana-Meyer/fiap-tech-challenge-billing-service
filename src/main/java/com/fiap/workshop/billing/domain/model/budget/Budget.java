package com.fiap.workshop.billing.domain.model.budget;

import com.fiap.workshop.billing.domain.exception.InvalidStateException;
import com.fiap.workshop.billing.domain.model.shared.Money;
import com.fiap.workshop.billing.domain.service.BudgetCalculationService;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** The budget of one service order. There is at most one per order. */
public class Budget {

    private final UUID id;
    private final UUID serviceOrderId;
    private final Instant createdAt;
    private List<BudgetItem> items;
    private Money totalAmount;
    private BudgetStatus status;
    private Instant updatedAt;

    public Budget(UUID id, UUID serviceOrderId, List<BudgetItem> items, Money totalAmount,
                  BudgetStatus status, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.serviceOrderId = serviceOrderId;
        this.items = List.copyOf(items);
        this.totalAmount = totalAmount;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Budget generate(UUID serviceOrderId, List<BudgetItem> items,
                                  BudgetCalculationService calculation) {
        requireItems(items);
        Instant now = Instant.now();
        return new Budget(UUID.randomUUID(), serviceOrderId, items, calculation.calculate(items),
                BudgetStatus.GENERATED, now, now);
    }

    /** A new diagnosis of the same order replaces the previous budget, unless it was already paid. */
    public void regenerate(List<BudgetItem> newItems, BudgetCalculationService calculation) {
        if (status == BudgetStatus.PAID) {
            throw new InvalidStateException("Budget of service order " + serviceOrderId + " is already paid");
        }
        requireItems(newItems);
        this.items = List.copyOf(newItems);
        this.totalAmount = calculation.calculate(newItems);
        this.status = BudgetStatus.GENERATED;
        touch();
    }

    public void ensurePayable() {
        if (status != BudgetStatus.GENERATED) {
            throw new InvalidStateException(
                    "Budget of service order " + serviceOrderId + " cannot be paid in status " + status);
        }
    }

    public void markPaid() {
        ensurePayable();
        this.status = BudgetStatus.PAID;
        touch();
    }

    /** Cancelling is idempotent; a paid budget has to be refunded instead. */
    public void cancel() {
        if (status == BudgetStatus.CANCELLED) {
            return;
        }
        if (status == BudgetStatus.PAID) {
            throw new InvalidStateException(
                    "Budget of service order " + serviceOrderId + " is paid; refund the payment instead");
        }
        this.status = BudgetStatus.CANCELLED;
        touch();
    }

    public void cancelAfterRefund() {
        if (status != BudgetStatus.PAID) {
            throw new InvalidStateException(
                    "Budget of service order " + serviceOrderId + " is not paid, status " + status);
        }
        this.status = BudgetStatus.CANCELLED;
        touch();
    }

    private static void requireItems(List<BudgetItem> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("A budget needs at least one item");
        }
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getServiceOrderId() { return serviceOrderId; }
    public List<BudgetItem> getItems() { return items; }
    public Money getTotalAmount() { return totalAmount; }
    public BudgetStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
