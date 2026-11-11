package com.fiap.workshop.billing.application.usecase.budget;

import com.fiap.workshop.billing.application.dto.budget.BudgetResponse;
import com.fiap.workshop.billing.application.port.in.budget.CancelBudgetInputPort;
import com.fiap.workshop.billing.domain.exception.ResourceNotFoundException;
import com.fiap.workshop.billing.domain.model.budget.Budget;
import com.fiap.workshop.billing.domain.repository.BudgetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CancelBudgetUseCase implements CancelBudgetInputPort {

    private final BudgetRepository budgetRepository;

    public CancelBudgetUseCase(BudgetRepository budgetRepository) {
        this.budgetRepository = budgetRepository;
    }

    @Transactional
    public BudgetResponse execute(UUID serviceOrderId) {
        Budget budget = budgetRepository.findByServiceOrderId(serviceOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget", serviceOrderId));
        budget.cancel();
        return BudgetResponse.from(budgetRepository.save(budget));
    }
}
