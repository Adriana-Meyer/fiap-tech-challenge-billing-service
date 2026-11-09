package com.fiap.workshop.billing.application.usecase.budget;

import com.fiap.workshop.billing.application.dto.budget.BudgetResponse;
import com.fiap.workshop.billing.application.port.in.budget.FindBudgetInputPort;
import com.fiap.workshop.billing.domain.exception.ResourceNotFoundException;
import com.fiap.workshop.billing.domain.repository.BudgetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class FindBudgetUseCase implements FindBudgetInputPort {

    private final BudgetRepository budgetRepository;

    public FindBudgetUseCase(BudgetRepository budgetRepository) {
        this.budgetRepository = budgetRepository;
    }

    @Transactional(readOnly = true)
    public BudgetResponse findByServiceOrderId(UUID serviceOrderId) {
        return budgetRepository.findByServiceOrderId(serviceOrderId)
                .map(BudgetResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Budget", serviceOrderId));
    }
}
