package com.fiap.workshop.billing.application.dto.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ProcessPaymentCommand(
        @NotNull UUID serviceOrderId,
        @NotBlank String paymentMethod
) {}
