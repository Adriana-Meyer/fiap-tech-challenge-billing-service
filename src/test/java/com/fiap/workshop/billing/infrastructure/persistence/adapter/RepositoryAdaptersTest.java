package com.fiap.workshop.billing.infrastructure.persistence.adapter;

import com.fiap.workshop.billing.domain.gateway.ChargeResult;
import com.fiap.workshop.billing.domain.model.budget.Budget;
import com.fiap.workshop.billing.domain.model.budget.BudgetItem;
import com.fiap.workshop.billing.domain.model.payment.Payment;
import com.fiap.workshop.billing.domain.model.shared.Money;
import com.fiap.workshop.billing.domain.service.BudgetCalculationService;
import com.fiap.workshop.billing.infrastructure.persistence.entity.BudgetJpaEntity;
import com.fiap.workshop.billing.infrastructure.persistence.entity.PaymentJpaEntity;
import com.fiap.workshop.billing.infrastructure.persistence.mapper.BudgetMapper;
import com.fiap.workshop.billing.infrastructure.persistence.mapper.PaymentMapper;
import com.fiap.workshop.billing.infrastructure.persistence.repository.BudgetJpaRepository;
import com.fiap.workshop.billing.infrastructure.persistence.repository.PaymentJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RepositoryAdaptersTest {

    @Mock
    private BudgetJpaRepository budgetJpaRepository;

    @Mock
    private PaymentJpaRepository paymentJpaRepository;

    private final BudgetMapper budgetMapper = new BudgetMapper();
    private final PaymentMapper paymentMapper = new PaymentMapper();
    private final UUID orderId = UUID.randomUUID();

    @Test
    void budgetAdapterShouldSaveAndFindThroughTheMapper() {
        BudgetRepositoryAdapter adapter = new BudgetRepositoryAdapter(budgetJpaRepository, budgetMapper);
        Budget budget = Budget.generate(orderId, List.of(new BudgetItem("Labor", Money.of(10), 1)),
                new BudgetCalculationService());
        BudgetJpaEntity entity = budgetMapper.toEntity(budget);
        when(budgetJpaRepository.save(any(BudgetJpaEntity.class))).thenReturn(entity);
        when(budgetJpaRepository.findByServiceOrderId(orderId)).thenReturn(Optional.of(entity));
        when(budgetJpaRepository.findByServiceOrderId(UUID.fromString("00000000-0000-0000-0000-000000000000")))
                .thenReturn(Optional.empty());

        assertEquals(budget.getId(), adapter.save(budget).getId());
        assertEquals(budget.getId(), adapter.findByServiceOrderId(orderId).orElseThrow().getId());
        assertTrue(adapter.findByServiceOrderId(UUID.fromString("00000000-0000-0000-0000-000000000000")).isEmpty());
    }

    @Test
    void paymentAdapterShouldSaveAndFindThroughTheMapper() {
        PaymentRepositoryAdapter adapter = new PaymentRepositoryAdapter(paymentJpaRepository, paymentMapper);
        Payment payment = Payment.fromCharge(orderId, UUID.randomUUID(), Money.of(10), "pix",
                ChargeResult.approved("MOCK-1"));
        PaymentJpaEntity entity = paymentMapper.toEntity(payment);
        when(paymentJpaRepository.save(any(PaymentJpaEntity.class))).thenReturn(entity);
        when(paymentJpaRepository.findByServiceOrderId(orderId)).thenReturn(Optional.of(entity));

        assertEquals(payment.getId(), adapter.save(payment).getId());
        assertEquals(payment.getId(), adapter.findByServiceOrderId(orderId).orElseThrow().getId());
    }
}
