package com.fiap.workshop.billing.interfaces.rest;

import com.fiap.workshop.billing.application.dto.budget.BudgetResponse;
import com.fiap.workshop.billing.application.port.in.budget.FindBudgetInputPort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Read-only: budgets are created and cancelled by saga commands, never by HTTP. */
@RestController
@RequestMapping("/api/v1/budgets")
public class BudgetController {

    private final FindBudgetInputPort findUseCase;

    public BudgetController(FindBudgetInputPort findUseCase) {
        this.findUseCase = findUseCase;
    }

    @GetMapping("/{serviceOrderId}")
    public BudgetResponse findByServiceOrderId(@PathVariable UUID serviceOrderId) {
        return findUseCase.findByServiceOrderId(serviceOrderId);
    }
}
