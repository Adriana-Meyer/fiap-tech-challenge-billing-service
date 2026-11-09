package com.fiap.workshop.billing.application.port.in.budget;

import com.fiap.workshop.billing.application.dto.budget.BudgetResponse;

import java.util.UUID;

public interface CancelBudgetInputPort {
    BudgetResponse execute(UUID serviceOrderId);
}
