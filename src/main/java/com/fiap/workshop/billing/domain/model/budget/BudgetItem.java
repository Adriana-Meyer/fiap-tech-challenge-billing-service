package com.fiap.workshop.billing.domain.model.budget;

import com.fiap.workshop.billing.domain.model.shared.Money;

/**
 * One line of a budget. Description and price are a snapshot sent by the OS
 * service: Billing never looks up the catalog or the supplies.
 */
public record BudgetItem(String description, Money unitPrice, int quantity) {

    public BudgetItem {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Budget item description is required");
        }
        if (unitPrice == null) {
            throw new IllegalArgumentException("Budget item unit price is required");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("Budget item quantity must be positive");
        }
    }

    public Money subtotal() {
        return unitPrice.multiply(quantity);
    }
}
