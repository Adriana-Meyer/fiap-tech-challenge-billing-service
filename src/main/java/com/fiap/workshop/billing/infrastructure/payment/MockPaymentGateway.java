package com.fiap.workshop.billing.infrastructure.payment;

import com.fiap.workshop.billing.domain.gateway.ChargeResult;
import com.fiap.workshop.billing.domain.gateway.PaymentGateway;
import com.fiap.workshop.billing.domain.model.shared.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Stand-in for Mercado Pago. Deterministic so the saga compensation can be
 * demonstrated on demand: a charge is declined when the payment method is
 * {@value #DECLINE_METHOD} or when the amount is above the configured limit.
 */
@Component
public class MockPaymentGateway implements PaymentGateway {

    static final String DECLINE_METHOD = "mock-decline";

    private static final Logger log = LoggerFactory.getLogger(MockPaymentGateway.class);

    private final Money declineAbove;

    public MockPaymentGateway(@Value("${app.payment.mock.decline-above:50000.00}") BigDecimal declineAbove) {
        this.declineAbove = Money.of(declineAbove);
    }

    @Override
    public ChargeResult charge(UUID serviceOrderId, Money amount, String paymentMethod) {
        if (DECLINE_METHOD.equals(paymentMethod)) {
            log.info("Mock payment declined for service order {}: method {}", serviceOrderId, paymentMethod);
            return ChargeResult.declined("Declined by payment method " + DECLINE_METHOD);
        }
        if (amount.isGreaterThan(declineAbove)) {
            log.info("Mock payment declined for service order {}: amount {} above limit {}",
                    serviceOrderId, amount, declineAbove);
            return ChargeResult.declined("Amount above the limit of " + declineAbove);
        }
        String reference = "MOCK-" + UUID.randomUUID();
        log.info("Mock payment approved for service order {}: {}", serviceOrderId, reference);
        return ChargeResult.approved(reference);
    }

    @Override
    public void refund(String externalReference) {
        log.info("Mock refund issued for payment {}", externalReference);
    }
}
