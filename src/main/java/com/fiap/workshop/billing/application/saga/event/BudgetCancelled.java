package com.fiap.workshop.billing.application.saga.event;

import java.util.UUID;

/** Compensation done. */
public record BudgetCancelled(UUID messageId, UUID sagaId, UUID serviceOrderId) implements SagaEvent {}
