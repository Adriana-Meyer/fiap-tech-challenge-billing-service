package com.fiap.workshop.billing.application.dto.budget;

import com.fiap.workshop.billing.domain.model.budget.BudgetItem;

import java.math.BigDecimal;

public record BudgetItemResponse(String description, BigDecimal unitPrice, int quantity, BigDecimal subtotal) {

    public static BudgetItemResponse from(BudgetItem item) {
        return new BudgetItemResponse(item.description(), item.unitPrice().getAmount(),
                item.quantity(), item.subtotal().getAmount());
    }
}
