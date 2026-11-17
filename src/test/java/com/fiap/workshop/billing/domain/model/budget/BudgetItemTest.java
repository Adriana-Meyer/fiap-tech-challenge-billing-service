package com.fiap.workshop.billing.domain.model.budget;

import com.fiap.workshop.billing.domain.model.shared.Money;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BudgetItemTest {

    @Test
    void shouldComputeSubtotal() {
        BudgetItem item = new BudgetItem("Oil change", Money.of(50.25), 3);
        assertEquals(Money.of(150.75), item.subtotal());
    }

    @Test
    void shouldRejectBlankDescription() {
        assertThrows(IllegalArgumentException.class, () -> new BudgetItem(" ", Money.of(1), 1));
        assertThrows(IllegalArgumentException.class, () -> new BudgetItem(null, Money.of(1), 1));
    }

    @Test
    void shouldRejectMissingPrice() {
        assertThrows(IllegalArgumentException.class, () -> new BudgetItem("x", null, 1));
    }

    @Test
    void shouldRejectNonPositiveQuantity() {
        assertThrows(IllegalArgumentException.class, () -> new BudgetItem("x", Money.of(1), 0));
        assertThrows(IllegalArgumentException.class, () -> new BudgetItem("x", Money.of(1), -2));
    }
}
