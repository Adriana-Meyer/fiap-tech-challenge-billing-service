package com.fiap.workshop.billing.application.dto.budget;

import com.fiap.workshop.billing.domain.model.budget.Budget;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BudgetResponse(
        UUID id,
        UUID serviceOrderId,
        List<BudgetItemResponse> items,
        BigDecimal totalAmount,
        String status,
        Instant createdAt,
        Instant updatedAt
) {

    public static BudgetResponse from(Budget budget) {
        return new BudgetResponse(
                budget.getId(),
                budget.getServiceOrderId(),
                budget.getItems().stream().map(BudgetItemResponse::from).toList(),
                budget.getTotalAmount().getAmount(),
                budget.getStatus().name(),
                budget.getCreatedAt(),
                budget.getUpdatedAt());
    }
}
