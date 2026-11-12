package com.fiap.workshop.billing.application.saga.event;

import java.util.UUID;

public record PaymentDeclined(UUID messageId, UUID sagaId, UUID serviceOrderId, String reason)
        implements SagaEvent {}
