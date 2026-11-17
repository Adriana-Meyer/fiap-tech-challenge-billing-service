package com.fiap.workshop.billing.infrastructure.persistence.adapter;

import com.fiap.workshop.billing.domain.model.payment.Payment;
import com.fiap.workshop.billing.domain.repository.PaymentRepository;
import com.fiap.workshop.billing.infrastructure.persistence.mapper.PaymentMapper;
import com.fiap.workshop.billing.infrastructure.persistence.repository.PaymentJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class PaymentRepositoryAdapter implements PaymentRepository {

    private final PaymentJpaRepository jpaRepository;
    private final PaymentMapper mapper;

    public PaymentRepositoryAdapter(PaymentJpaRepository jpaRepository, PaymentMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Payment save(Payment payment) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(payment)));
    }

    @Override
    public Optional<Payment> findByServiceOrderId(UUID serviceOrderId) {
        return jpaRepository.findByServiceOrderId(serviceOrderId).map(mapper::toDomain);
    }
}
