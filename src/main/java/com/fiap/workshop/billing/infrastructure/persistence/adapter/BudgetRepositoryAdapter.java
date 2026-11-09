package com.fiap.workshop.billing.infrastructure.persistence.adapter;

import com.fiap.workshop.billing.domain.model.budget.Budget;
import com.fiap.workshop.billing.domain.repository.BudgetRepository;
import com.fiap.workshop.billing.infrastructure.persistence.mapper.BudgetMapper;
import com.fiap.workshop.billing.infrastructure.persistence.repository.BudgetJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class BudgetRepositoryAdapter implements BudgetRepository {

    private final BudgetJpaRepository jpaRepository;
    private final BudgetMapper mapper;

    public BudgetRepositoryAdapter(BudgetJpaRepository jpaRepository, BudgetMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Budget save(Budget budget) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(budget)));
    }

    @Override
    public Optional<Budget> findByServiceOrderId(UUID serviceOrderId) {
        return jpaRepository.findByServiceOrderId(serviceOrderId).map(mapper::toDomain);
    }
}
