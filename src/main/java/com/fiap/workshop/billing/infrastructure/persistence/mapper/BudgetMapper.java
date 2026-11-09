package com.fiap.workshop.billing.infrastructure.persistence.mapper;

import com.fiap.workshop.billing.domain.model.budget.Budget;
import com.fiap.workshop.billing.domain.model.budget.BudgetItem;
import com.fiap.workshop.billing.domain.model.budget.BudgetStatus;
import com.fiap.workshop.billing.domain.model.shared.Money;
import com.fiap.workshop.billing.infrastructure.persistence.entity.BudgetItemJpaEntity;
import com.fiap.workshop.billing.infrastructure.persistence.entity.BudgetJpaEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

@Component
public class BudgetMapper {

    public Budget toDomain(BudgetJpaEntity e) {
        List<BudgetItem> items = e.getItems().stream()
                .map(i -> new BudgetItem(i.getDescription(), Money.of(i.getUnitPrice()), i.getQuantity()))
                .toList();
        return new Budget(e.getId(), e.getServiceOrderId(), items, Money.of(e.getTotalAmount()),
                BudgetStatus.valueOf(e.getStatus()), e.getCreatedAt(), e.getUpdatedAt());
    }

    public BudgetJpaEntity toEntity(Budget b) {
        List<BudgetItem> items = b.getItems();
        List<BudgetItemJpaEntity> itemEntities = IntStream.range(0, items.size())
                .mapToObj(i -> new BudgetItemJpaEntity(UUID.randomUUID(), i, items.get(i).description(),
                        items.get(i).unitPrice().getAmount(), items.get(i).quantity()))
                .toList();
        return new BudgetJpaEntity(b.getId(), b.getServiceOrderId(), b.getStatus().name(),
                b.getTotalAmount().getAmount(), b.getCreatedAt(), b.getUpdatedAt(), itemEntities);
    }
}
