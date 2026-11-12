package com.fiap.workshop.billing.application.saga.command;

import java.util.UUID;

/** Compensation: cancel the budget of an order that will not be executed. */
public record CancelBudget(UUID sagaId, UUID serviceOrderId) implements SagaCommand {}
