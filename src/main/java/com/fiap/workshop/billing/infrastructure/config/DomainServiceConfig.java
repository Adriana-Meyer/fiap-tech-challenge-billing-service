package com.fiap.workshop.billing.infrastructure.config;

import com.fiap.workshop.billing.domain.service.BudgetCalculationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainServiceConfig {

    @Bean
    public BudgetCalculationService budgetCalculationService() {
        return new BudgetCalculationService();
    }
}
