package com.fiap.workshop.billing.application.port.in.budget;

import com.fiap.workshop.billing.application.dto.budget.BudgetResponse;

import java.util.UUID;

public interface FindBudgetInputPort {
    BudgetResponse findByServiceOrderId(UUID serviceOrderId);
}
