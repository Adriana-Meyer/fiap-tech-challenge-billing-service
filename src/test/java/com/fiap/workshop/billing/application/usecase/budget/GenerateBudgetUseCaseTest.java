package com.fiap.workshop.billing.application.usecase.budget;

import com.fiap.workshop.billing.application.dto.budget.BudgetItemCommand;
import com.fiap.workshop.billing.application.dto.budget.BudgetResponse;
import com.fiap.workshop.billing.application.dto.budget.GenerateBudgetCommand;
import com.fiap.workshop.billing.domain.exception.InvalidStateException;
import com.fiap.workshop.billing.domain.model.budget.Budget;
import com.fiap.workshop.billing.domain.model.budget.BudgetItem;
import com.fiap.workshop.billing.domain.model.budget.BudgetStatus;
import com.fiap.workshop.billing.domain.model.shared.Money;
import com.fiap.workshop.billing.domain.repository.BudgetRepository;
import com.fiap.workshop.billing.domain.service.BudgetCalculationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenerateBudgetUseCaseTest {

    @Mock
    private BudgetRepository budgetRepository;

    @Spy
    private BudgetCalculationService calculationService = new BudgetCalculationService();

    @InjectMocks
    private GenerateBudgetUseCase useCase;

    private final UUID orderId = UUID.randomUUID();

    private GenerateBudgetCommand command(double price, int quantity) {
        return new GenerateBudgetCommand(orderId,
                List.of(new BudgetItemCommand("Labor", BigDecimal.valueOf(price), quantity)));
    }

    @BeforeEach
    void echoSave() {
        org.mockito.Mockito.lenient().when(budgetRepository.save(any(Budget.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldCreateABudgetWhenNoneExists() {
        when(budgetRepository.findByServiceOrderId(orderId)).thenReturn(Optional.empty());

        BudgetResponse response = useCase.execute(command(100, 2));

        assertEquals(orderId, response.serviceOrderId());
        assertEquals(0, new BigDecimal("200.00").compareTo(response.totalAmount()));
        assertEquals("GENERATED", response.status());
        assertEquals(1, response.items().size());
    }

    @Test
    void shouldRecalculateTheExistingBudgetInsteadOfCreatingAnother() {
        Budget existing = Budget.generate(orderId,
                List.of(new BudgetItem("Old", Money.of(10), 1)), calculationService);
        when(budgetRepository.findByServiceOrderId(orderId)).thenReturn(Optional.of(existing));

        BudgetResponse response = useCase.execute(command(100, 3));

        assertEquals(existing.getId(), response.id());
        assertEquals(0, new BigDecimal("300.00").compareTo(response.totalAmount()));
    }

    @Test
    void shouldRefuseToRegenerateAPaidBudget() {
        Budget paid = Budget.generate(orderId,
                List.of(new BudgetItem("Old", Money.of(10), 1)), calculationService);
        paid.markPaid();
        when(budgetRepository.findByServiceOrderId(orderId)).thenReturn(Optional.of(paid));

        assertThrows(InvalidStateException.class, () -> useCase.execute(command(100, 1)));

        verify(budgetRepository, never()).save(any());
        assertEquals(BudgetStatus.PAID, paid.getStatus());
    }
}
