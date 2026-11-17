package com.fiap.workshop.billing.application.dto.payment;

import com.fiap.workshop.billing.domain.model.payment.Payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID serviceOrderId,
        UUID budgetId,
        BigDecimal amount,
        String paymentMethod,
        String status,
        String externalReference,
        String declineReason,
        Instant createdAt,
        Instant updatedAt
) {

    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getServiceOrderId(),
                payment.getBudgetId(),
                payment.getAmount().getAmount(),
                payment.getPaymentMethod(),
                payment.getStatus().name(),
                payment.getExternalReference(),
                payment.getDeclineReason(),
                payment.getCreatedAt(),
                payment.getUpdatedAt());
    }
}
