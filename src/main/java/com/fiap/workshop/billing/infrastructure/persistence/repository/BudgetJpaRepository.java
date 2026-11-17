package com.fiap.workshop.billing.infrastructure.persistence.repository;

import com.fiap.workshop.billing.infrastructure.persistence.entity.BudgetJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BudgetJpaRepository extends JpaRepository<BudgetJpaEntity, UUID> {

    Optional<BudgetJpaEntity> findByServiceOrderId(UUID serviceOrderId);
}
