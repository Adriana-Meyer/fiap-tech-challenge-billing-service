package com.fiap.workshop.billing.application.dto.budget;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record BudgetItemCommand(
        @NotBlank String description,
        @NotNull @PositiveOrZero BigDecimal unitPrice,
        @Positive int quantity
) {}
