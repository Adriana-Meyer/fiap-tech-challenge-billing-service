package com.fiap.workshop.billing.application.usecase.budget;

import com.fiap.workshop.billing.application.dto.budget.BudgetResponse;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindBudgetUseCaseTest {

    @Mock
    private BudgetRepository budgetRepository;

    @InjectMocks
    private FindBudgetUseCase useCase;

    private final UUID orderId = UUID.randomUUID();

    @Test
    void shouldFindTheBudgetOfAnOrder() {
        Budget budget = Budget.generate(orderId, List.of(new BudgetItem("Labor", Money.of(10), 2)),
                new BudgetCalculationService());
        when(budgetRepository.findByServiceOrderId(orderId)).thenReturn(Optional.of(budget));

        BudgetResponse response = useCase.findByServiceOrderId(orderId);

        assertEquals(budget.getId(), response.id());
        assertEquals("Labor", response.items().get(0).description());
    }

    @Test
    void shouldFailWhenThereIsNoBudget() {
        when(budgetRepository.findByServiceOrderId(orderId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> useCase.findByServiceOrderId(orderId));
    }
}
