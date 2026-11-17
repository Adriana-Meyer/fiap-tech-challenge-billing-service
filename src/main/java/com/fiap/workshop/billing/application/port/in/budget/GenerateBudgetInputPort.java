package com.fiap.workshop.billing.application.port.in.budget;

import com.fiap.workshop.billing.application.dto.budget.BudgetResponse;
import com.fiap.workshop.billing.application.dto.budget.GenerateBudgetCommand;

public interface GenerateBudgetInputPort {
    BudgetResponse execute(GenerateBudgetCommand command);
}
