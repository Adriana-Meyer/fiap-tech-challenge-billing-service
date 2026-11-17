package com.fiap.workshop.billing.application.saga.event;

import java.math.BigDecimal;
import java.util.UUID;

public record BudgetGenerated(UUID messageId, UUID sagaId, UUID serviceOrderId, BigDecimal totalAmount)
        implements SagaEvent {}
