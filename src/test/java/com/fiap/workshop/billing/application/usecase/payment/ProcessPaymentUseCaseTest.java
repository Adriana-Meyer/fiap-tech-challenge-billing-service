package com.fiap.workshop.billing.application.usecase.payment;

import com.fiap.workshop.billing.application.dto.payment.PaymentResponse;
import com.fiap.workshop.billing.application.dto.payment.ProcessPaymentCommand;
import com.fiap.workshop.billing.domain.exception.InvalidStateException;
import com.fiap.workshop.billing.domain.exception.ResourceNotFoundException;
import com.fiap.workshop.billing.domain.gateway.ChargeResult;
import com.fiap.workshop.billing.domain.gateway.PaymentGateway;
import com.fiap.workshop.billing.domain.model.budget.Budget;
import com.fiap.workshop.billing.domain.model.budget.BudgetItem;
import com.fiap.workshop.billing.domain.model.budget.BudgetStatus;
import com.fiap.workshop.billing.domain.model.payment.Payment;
import com.fiap.workshop.billing.domain.model.shared.Money;
import com.fiap.workshop.billing.domain.repository.BudgetRepository;
import com.fiap.workshop.billing.domain.repository.PaymentRepository;
import com.fiap.workshop.billing.domain.service.BudgetCalculationService;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessPaymentUseCaseTest {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentGateway paymentGateway;

    @InjectMocks
    private ProcessPaymentUseCase useCase;

    private final UUID orderId = UUID.randomUUID();
    private final ProcessPaymentCommand command = new ProcessPaymentCommand(orderId, "credit-card");

    private Budget budget() {
        return Budget.generate(orderId, List.of(new BudgetItem("Labor", Money.of(100), 2)),
                new BudgetCalculationService());
    }

    @BeforeEach
    void echoSaves() {
        lenient().when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));
        lenient().when(budgetRepository.save(any(Budget.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void shouldApproveAndMarkTheBudgetPaid() {
        Budget budget = budget();
        when(paymentRepository.findByServiceOrderId(orderId)).thenReturn(Optional.empty());
        when(budgetRepository.findByServiceOrderId(orderId)).thenReturn(Optional.of(budget));
        when(paymentGateway.charge(orderId, Money.of(200), "credit-card"))
                .thenReturn(ChargeResult.approved("MOCK-1"));

        PaymentResponse response = useCase.execute(command);

        assertEquals("APPROVED", response.status());
        assertEquals("MOCK-1", response.externalReference());
        assertEquals(budget.getId(), response.budgetId());
        assertEquals(BudgetStatus.PAID, budget.getStatus());
        verify(budgetRepository).save(budget);
    }

    @Test
    void shouldReturnADeclinedPaymentAndLeaveTheBudgetUntouched() {
        Budget budget = budget();
        when(paymentRepository.findByServiceOrderId(orderId)).thenReturn(Optional.empty());
        when(budgetRepository.findByServiceOrderId(orderId)).thenReturn(Optional.of(budget));
        when(paymentGateway.charge(orderId, Money.of(200), "credit-card"))
                .thenReturn(ChargeResult.declined("no funds"));

        PaymentResponse response = useCase.execute(command);

        assertEquals("DECLINED", response.status());
        assertEquals("no funds", response.declineReason());
        assertEquals(BudgetStatus.GENERATED, budget.getStatus());
        verify(budgetRepository, never()).save(any());
    }

    @Test
    void shouldReturnTheExistingPaymentWithoutChargingAgain() {
        Payment existing = Payment.fromCharge(orderId, UUID.randomUUID(), Money.of(200), "credit-card",
                ChargeResult.approved("MOCK-1"));
        when(paymentRepository.findByServiceOrderId(orderId)).thenReturn(Optional.of(existing));

        PaymentResponse response = useCase.execute(command);

        assertEquals(existing.getId(), response.id());
        verify(paymentGateway, never()).charge(any(), any(), any());
    }

    @Test
    void shouldFailWhenThereIsNoBudgetToCharge() {
        when(paymentRepository.findByServiceOrderId(orderId)).thenReturn(Optional.empty());
        when(budgetRepository.findByServiceOrderId(orderId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(command));
        verify(paymentGateway, never()).charge(any(), any(), any());
    }

    @Test
    void shouldNotChargeACancelledBudget() {
        Budget budget = budget();
        budget.cancel();
        when(paymentRepository.findByServiceOrderId(orderId)).thenReturn(Optional.empty());
        when(budgetRepository.findByServiceOrderId(orderId)).thenReturn(Optional.of(budget));

        assertThrows(InvalidStateException.class, () -> useCase.execute(command));
        verify(paymentGateway, never()).charge(any(), any(), any());
    }
}
