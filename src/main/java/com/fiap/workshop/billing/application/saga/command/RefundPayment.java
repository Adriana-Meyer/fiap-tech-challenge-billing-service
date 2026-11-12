package com.fiap.workshop.billing.application.saga.command;

import java.util.UUID;

/** Compensation: refund the charge (this also cancels the budget). */
public record RefundPayment(UUID sagaId, UUID serviceOrderId) implements SagaCommand {}
