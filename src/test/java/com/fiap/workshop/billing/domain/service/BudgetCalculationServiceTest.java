package com.fiap.workshop.billing.domain.service;

import com.fiap.workshop.billing.domain.model.budget.BudgetItem;
import com.fiap.workshop.billing.domain.model.shared.Money;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BudgetCalculationServiceTest {

    private final BudgetCalculationService service = new BudgetCalculationService();

    @Test
    void shouldSumTheSubtotals() {
        List<BudgetItem> items = List.of(
                new BudgetItem("Labor", Money.of(100), 2),
                new BudgetItem("Filter", Money.of(35.50), 1));
        assertEquals(Money.of(235.50), service.calculate(items));
    }

    @Test
    void shouldReturnZeroForNoItems() {
        assertEquals(Money.zero(), service.calculate(List.of()));
    }
}
