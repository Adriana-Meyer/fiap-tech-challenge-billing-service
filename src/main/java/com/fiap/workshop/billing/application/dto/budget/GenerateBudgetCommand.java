package com.fiap.workshop.billing.application.dto.budget;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record GenerateBudgetCommand(
        @NotNull UUID serviceOrderId,
        @NotEmpty @Valid List<BudgetItemCommand> items
) {}
