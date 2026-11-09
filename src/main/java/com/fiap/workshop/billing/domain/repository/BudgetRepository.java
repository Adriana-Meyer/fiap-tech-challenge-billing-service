package com.fiap.workshop.billing.domain.repository;

import com.fiap.workshop.billing.domain.model.budget.Budget;

import java.util.Optional;
import java.util.UUID;

public interface BudgetRepository {

    Budget save(Budget budget);

    Optional<Budget> findByServiceOrderId(UUID serviceOrderId);
}
