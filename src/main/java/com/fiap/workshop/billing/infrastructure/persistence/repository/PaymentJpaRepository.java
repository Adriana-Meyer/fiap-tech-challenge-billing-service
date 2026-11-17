package com.fiap.workshop.billing.infrastructure.persistence.repository;

import com.fiap.workshop.billing.infrastructure.persistence.entity.PaymentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PaymentJpaRepository extends JpaRepository<PaymentJpaEntity, UUID> {

    Optional<PaymentJpaEntity> findByServiceOrderId(UUID serviceOrderId);
}
