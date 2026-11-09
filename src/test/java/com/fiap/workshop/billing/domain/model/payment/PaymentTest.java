package com.fiap.workshop.billing.domain.model.payment;

import com.fiap.workshop.billing.domain.exception.InvalidStateException;
import com.fiap.workshop.billing.domain.gateway.ChargeResult;
import com.fiap.workshop.billing.domain.model.shared.Money;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaymentTest {

    private Payment payment(ChargeResult result) {
        return Payment.fromCharge(UUID.randomUUID(), UUID.randomUUID(), Money.of(120), "credit-card", result);
    }

    @Test
    void shouldBeApprovedWhenTheGatewayApproves() {
        Payment payment = payment(ChargeResult.approved("MOCK-1"));

        assertEquals(PaymentStatus.APPROVED, payment.getStatus());
        assertEquals("MOCK-1", payment.getExternalReference());
        assertNull(payment.getDeclineReason());
        assertEquals(Money.of(120), payment.getAmount());
    }

    @Test
    void shouldBeDeclinedWhenTheGatewayDeclines() {
        Payment payment = payment(ChargeResult.declined("no funds"));

        assertEquals(PaymentStatus.DECLINED, payment.getStatus());
        assertEquals("no funds", payment.getDeclineReason());
        assertNull(payment.getExternalReference());
    }

    @Test
    void shouldRefundAnApprovedPayment() {
        Payment payment = payment(ChargeResult.approved("MOCK-1"));

        payment.refund();

        assertEquals(PaymentStatus.REFUNDED, payment.getStatus());
    }

    @Test
    void shouldNotRefundADeclinedOrAlreadyRefundedPayment() {
        Payment declined = payment(ChargeResult.declined("no funds"));
        assertThrows(InvalidStateException.class, declined::refund);

        Payment refunded = payment(ChargeResult.approved("MOCK-1"));
        refunded.refund();
        assertThrows(InvalidStateException.class, refunded::refund);
    }
}
