package com.fiap.workshop.billing.application.saga.command;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Calculate the budget of a diagnosed order from a snapshot of its items. */
public record GenerateBudget(UUID sagaId, UUID serviceOrderId, List<Line> items) implements SagaCommand {

    public record Line(String description, BigDecimal unitPrice, int quantity) {}
}
