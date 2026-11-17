package com.fiap.workshop.billing.application.usecase.budget;

import com.fiap.workshop.billing.application.dto.budget.BudgetItemCommand;
import com.fiap.workshop.billing.application.dto.budget.BudgetResponse;
import com.fiap.workshop.billing.application.dto.budget.GenerateBudgetCommand;
import com.fiap.workshop.billing.application.port.in.budget.GenerateBudgetInputPort;
import com.fiap.workshop.billing.domain.model.budget.Budget;
import com.fiap.workshop.billing.domain.model.budget.BudgetItem;
import com.fiap.workshop.billing.domain.model.shared.Money;
import com.fiap.workshop.billing.domain.repository.BudgetRepository;
import com.fiap.workshop.billing.domain.service.BudgetCalculationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Idempotent per service order: a repeated command (message redelivery or a
 * new diagnosis) recalculates the existing budget instead of creating another.
 */
@Service
public class GenerateBudgetUseCase implements GenerateBudgetInputPort {

    private final BudgetRepository budgetRepository;
    private final BudgetCalculationService calculationService;

    public GenerateBudgetUseCase(BudgetRepository budgetRepository,
                                 BudgetCalculationService calculationService) {
        this.budgetRepository = budgetRepository;
        this.calculationService = calculationService;
    }

    @Transactional
    public BudgetResponse execute(GenerateBudgetCommand command) {
        List<BudgetItem> items = command.items().stream().map(GenerateBudgetUseCase::toItem).toList();

        Budget budget = budgetRepository.findByServiceOrderId(command.serviceOrderId())
                .map(existing -> {
                    existing.regenerate(items, calculationService);
                    return existing;
                })
                .orElseGet(() -> Budget.generate(command.serviceOrderId(), items, calculationService));

        return BudgetResponse.from(budgetRepository.save(budget));
    }

    private static BudgetItem toItem(BudgetItemCommand item) {
        return new BudgetItem(item.description(), Money.of(item.unitPrice()), item.quantity());
    }
}
