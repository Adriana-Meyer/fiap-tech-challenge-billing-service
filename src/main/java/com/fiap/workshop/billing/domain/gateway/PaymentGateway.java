package com.fiap.workshop.billing.domain.gateway;

import com.fiap.workshop.billing.domain.model.shared.Money;

import java.util.UUID;

/** Port to the payment provider (Mercado Pago in production, a deterministic mock here). */
public interface PaymentGateway {

    ChargeResult charge(UUID serviceOrderId, Money amount, String paymentMethod);

    void refund(String externalReference);
}
