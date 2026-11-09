package com.fiap.workshop.billing.application.port.in.payment;

import com.fiap.workshop.billing.application.dto.payment.PaymentResponse;

import java.util.UUID;

public interface FindPaymentInputPort {
    PaymentResponse findByServiceOrderId(UUID serviceOrderId);
}
