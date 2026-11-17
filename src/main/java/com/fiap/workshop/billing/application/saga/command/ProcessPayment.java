package com.fiap.workshop.billing.application.saga.command;

import java.util.UUID;

/** Charge the approved budget. */
public record ProcessPayment(UUID sagaId, UUID serviceOrderId, String paymentMethod) implements SagaCommand {}
