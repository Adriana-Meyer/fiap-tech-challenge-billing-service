package com.fiap.workshop.billing.application.usecase.budget;

import com.fiap.workshop.billing.application.dto.budget.BudgetResponse;
import com.fiap.workshop.billing.domain.exception.InvalidStateException;
import com.fiap.workshop.billing.domain.exception.ResourceNotFoundException;
import com.fiap.workshop.billing.domain.model.budget.Budget;
import com.fiap.workshop.billing.domain.model.budget.BudgetItem;
import com.fiap.workshop.billing.domain.model.shared.Money;
import com.fiap.workshop.billing.domain.repository.BudgetRepository;
import com.fiap.workshop.billing.domain.service.BudgetCalculationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class CancelBudgetUseCaseTest {

    @Mock
    private BudgetRepository budgetRepository;

    @InjectMocks
    private CancelBudgetUseCase useCase;

    private final UUID orderId = UUID.randomUUID();

    private Budget budget() {
        return Budget.generate(orderId, List.of(new BudgetItem("Labor", Money.of(10), 1)),
                new BudgetCalculationService());
    }

    @Test
    void shouldCancelTheBudget() {
        when(budgetRepository.findByServiceOrderId(orderId)).thenReturn(Optional.of(budget()));
        when(budgetRepository.save(any(Budget.class))).thenAnswer(i -> i.getArgument(0));

        BudgetResponse response = useCase.execute(orderId);

        assertEquals("CANCELLED", response.status());
    }

    @Test
    void shouldFailWhenThereIsNoBudget() {
        when(budgetRepository.findByServiceOrderId(orderId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(orderId));
    }

    @Test
    void shouldNotCancelAPaidBudget() {
        Budget paid = budget();
        paid.markPaid();
        when(budgetRepository.findByServiceOrderId(orderId)).thenReturn(Optional.of(paid));

        assertThrows(InvalidStateException.class, () -> useCase.execute(orderId));
        verify(budgetRepository, never()).save(any());
    }
}
