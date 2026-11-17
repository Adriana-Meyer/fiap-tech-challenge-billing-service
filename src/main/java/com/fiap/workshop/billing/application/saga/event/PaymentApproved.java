package com.fiap.workshop.billing.application.saga.event;

import java.util.UUID;

public record PaymentApproved(UUID messageId, UUID sagaId, UUID serviceOrderId) implements SagaEvent {}
