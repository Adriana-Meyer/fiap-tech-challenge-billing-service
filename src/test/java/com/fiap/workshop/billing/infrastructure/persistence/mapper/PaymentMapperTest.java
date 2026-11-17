package com.fiap.workshop.billing.infrastructure.persistence.mapper;

import com.fiap.workshop.billing.domain.gateway.ChargeResult;
import com.fiap.workshop.billing.domain.model.payment.Payment;
import com.fiap.workshop.billing.domain.model.shared.Money;
import com.fiap.workshop.billing.infrastructure.persistence.entity.PaymentJpaEntity;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaymentMapperTest {

    private final PaymentMapper mapper = new PaymentMapper();

    @Test
    void shouldRoundTripAnApprovedPayment() {
        Payment payment = Payment.fromCharge(UUID.randomUUID(), UUID.randomUUID(), Money.of(80),
                "credit-card", ChargeResult.approved("MOCK-1"));

        Payment back = mapper.toDomain(mapper.toEntity(payment));

        assertEquals(payment.getId(), back.getId());
        assertEquals(payment.getBudgetId(), back.getBudgetId());
        assertEquals(payment.getAmount(), back.getAmount());
        assertEquals(payment.getStatus(), back.getStatus());
        assertEquals("MOCK-1", back.getExternalReference());
    }

    @Test
    void shouldRoundTripADeclinedPayment() {
        Payment payment = Payment.fromCharge(UUID.randomUUID(), UUID.randomUUID(), Money.of(80),
                "mock-decline", ChargeResult.declined("no funds"));

        PaymentJpaEntity entity = mapper.toEntity(payment);
        Payment back = mapper.toDomain(entity);

        assertEquals("DECLINED", entity.getStatus());
        assertEquals("no funds", back.getDeclineReason());
    }
}
