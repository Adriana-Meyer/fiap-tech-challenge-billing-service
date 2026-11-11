package com.fiap.workshop.billing.infrastructure.payment;

import com.fiap.workshop.billing.domain.gateway.ChargeResult;
import com.fiap.workshop.billing.domain.model.shared.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MockPaymentGatewayTest {

    private final MockPaymentGateway gateway = new MockPaymentGateway(new BigDecimal("1000.00"));
    private final UUID orderId = UUID.randomUUID();

    @Test
    void shouldApproveWithinTheLimit() {
        ChargeResult result = gateway.charge(orderId, Money.of(1000), "credit-card");

        assertTrue(result.approved());
        assertTrue(result.externalReference().startsWith("MOCK-"));
    }

    @Test
    void shouldGenerateADifferentReferenceEachTime() {
        String first = gateway.charge(orderId, Money.of(10), "pix").externalReference();
        String second = gateway.charge(orderId, Money.of(10), "pix").externalReference();

        assertNotEquals(first, second);
    }

    @Test
    void shouldDeclineAboveTheLimit() {
        ChargeResult result = gateway.charge(orderId, Money.of(1000.01), "credit-card");

        assertFalse(result.approved());
        assertTrue(result.declineReason().contains("limit"));
    }

    @Test
    void shouldDeclineTheMockDeclineMethod() {
        ChargeResult result = gateway.charge(orderId, Money.of(1), "mock-decline");

        assertFalse(result.approved());
        assertTrue(result.declineReason().contains("mock-decline"));
    }

    @Test
    void shouldAcceptARefund() {
        assertDoesNotThrow(() -> gateway.refund("MOCK-1"));
    }
}
