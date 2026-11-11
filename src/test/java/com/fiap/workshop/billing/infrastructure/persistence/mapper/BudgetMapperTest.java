package com.fiap.workshop.billing.infrastructure.persistence.mapper;

import com.fiap.workshop.billing.domain.model.budget.Budget;
import com.fiap.workshop.billing.domain.model.budget.BudgetItem;
import com.fiap.workshop.billing.domain.model.shared.Money;
import com.fiap.workshop.billing.domain.service.BudgetCalculationService;
import com.fiap.workshop.billing.infrastructure.persistence.entity.BudgetJpaEntity;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BudgetMapperTest {

    private final BudgetMapper mapper = new BudgetMapper();

    @Test
    void shouldRoundTripABudgetKeepingTheItemOrder() {
        Budget budget = Budget.generate(UUID.randomUUID(), List.of(
                new BudgetItem("Labor", Money.of(100), 2),
                new BudgetItem("Filter", Money.of(35.5), 1)), new BudgetCalculationService());

        BudgetJpaEntity entity = mapper.toEntity(budget);
        Budget back = mapper.toDomain(entity);

        assertEquals(0, entity.getItems().get(0).getPosition());
        assertEquals(1, entity.getItems().get(1).getPosition());
        assertEquals(budget.getId(), back.getId());
        assertEquals(budget.getServiceOrderId(), back.getServiceOrderId());
        assertEquals(budget.getItems(), back.getItems());
        assertEquals(budget.getTotalAmount(), back.getTotalAmount());
        assertEquals(budget.getStatus(), back.getStatus());
        assertEquals(budget.getCreatedAt(), back.getCreatedAt());
    }
}
