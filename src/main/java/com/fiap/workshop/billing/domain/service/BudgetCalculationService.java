package com.fiap.workshop.billing.domain.service;

import com.fiap.workshop.billing.domain.model.budget.BudgetItem;
import com.fiap.workshop.billing.domain.model.shared.Money;

import java.util.List;

public class BudgetCalculationService {

    public Money calculate(List<BudgetItem> items) {
        return items.stream()
                .map(BudgetItem::subtotal)
                .reduce(Money.zero(), Money::add);
    }
}
