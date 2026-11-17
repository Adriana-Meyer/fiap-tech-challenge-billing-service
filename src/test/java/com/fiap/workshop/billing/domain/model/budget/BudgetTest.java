package com.fiap.workshop.billing.domain.model.budget;

import com.fiap.workshop.billing.domain.exception.InvalidStateException;
import com.fiap.workshop.billing.domain.model.shared.Money;
import com.fiap.workshop.billing.domain.service.BudgetCalculationService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BudgetTest {

    private final BudgetCalculationService calculation = new BudgetCalculationService();
    private final UUID orderId = UUID.randomUUID();

    private List<BudgetItem> items(double price) {
        return List.of(new BudgetItem("Labor", Money.of(price), 2));
    }

    private Budget generated() {
        return Budget.generate(orderId, items(100), calculation);
    }

    @Test
    void shouldGenerateWithTotalAndGeneratedStatus() {
        Budget budget = generated();

        assertNotNull(budget.getId());
        assertEquals(orderId, budget.getServiceOrderId());
        assertEquals(Money.of(200), budget.getTotalAmount());
        assertEquals(BudgetStatus.GENERATED, budget.getStatus());
        assertEquals(budget.getCreatedAt(), budget.getUpdatedAt());
    }

    @Test
    void shouldRejectABudgetWithoutItems() {
        assertThrows(IllegalArgumentException.class, () -> Budget.generate(orderId, List.of(), calculation));
        assertThrows(IllegalArgumentException.class, () -> Budget.generate(orderId, null, calculation));
    }

    @Test
    void shouldRegenerateAnUnpaidBudget() {
        Budget budget = generated();
        budget.cancel();

        budget.regenerate(items(30), calculation);

        assertEquals(Money.of(60), budget.getTotalAmount());
        assertEquals(BudgetStatus.GENERATED, budget.getStatus());
    }

    @Test
    void shouldNotRegenerateAPaidBudget() {
        Budget budget = generated();
        budget.markPaid();

        assertThrows(InvalidStateException.class, () -> budget.regenerate(items(30), calculation));
        assertEquals(Money.of(200), budget.getTotalAmount());
    }

    @Test
    void shouldMarkPaidOnlyFromGenerated() {
        Budget budget = generated();
        budget.markPaid();
        assertEquals(BudgetStatus.PAID, budget.getStatus());

        assertThrows(InvalidStateException.class, budget::markPaid);
    }

    @Test
    void shouldNotBePayableWhenCancelled() {
        Budget budget = generated();
        budget.cancel();

        assertThrows(InvalidStateException.class, budget::ensurePayable);
    }

    @Test
    void shouldCancelIdempotently() {
        Budget budget = generated();
        budget.cancel();
        budget.cancel();

        assertEquals(BudgetStatus.CANCELLED, budget.getStatus());
    }

    @Test
    void shouldNotCancelAPaidBudget() {
        Budget budget = generated();
        budget.markPaid();

        assertThrows(InvalidStateException.class, budget::cancel);
        assertEquals(BudgetStatus.PAID, budget.getStatus());
    }

    @Test
    void shouldCancelAfterRefundOnlyWhenPaid() {
        Budget unpaid = generated();
        assertThrows(InvalidStateException.class, unpaid::cancelAfterRefund);

        Budget paid = generated();
        paid.markPaid();
        paid.cancelAfterRefund();
        assertEquals(BudgetStatus.CANCELLED, paid.getStatus());
    }
}
