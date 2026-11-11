package com.fiap.workshop.billing.domain.repository;

import com.fiap.workshop.billing.domain.model.payment.Payment;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository {

    Payment save(Payment payment);

    Optional<Payment> findByServiceOrderId(UUID serviceOrderId);
}
