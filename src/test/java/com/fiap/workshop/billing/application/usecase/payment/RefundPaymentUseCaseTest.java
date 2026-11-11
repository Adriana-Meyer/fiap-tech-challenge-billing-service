package com.fiap.workshop.billing.application.usecase.payment;

import com.fiap.workshop.billing.application.dto.payment.PaymentResponse;
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
class RefundPaymentUseCaseTest {

    @Mock
    private BudgetRepository budgetRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentGateway paymentGateway;

    @InjectMocks
    private RefundPaymentUseCase useCase;

    private final UUID orderId = UUID.randomUUID();

    private Budget paidBudget() {
        Budget budget = Budget.generate(orderId, List.of(new BudgetItem("Labor", Money.of(100), 1)),
                new BudgetCalculationService());
        budget.markPaid();
        return budget;
    }

    private Payment approvedPayment() {
        return Payment.fromCharge(orderId, UUID.randomUUID(), Money.of(100), "credit-card",
                ChargeResult.approved("MOCK-1"));
    }

    @Test
    void shouldRefundThePaymentAndCancelTheBudget() {
        Payment payment = approvedPayment();
        Budget budget = paidBudget();
        when(paymentRepository.findByServiceOrderId(orderId)).thenReturn(Optional.of(payment));
        when(budgetRepository.findByServiceOrderId(orderId)).thenReturn(Optional.of(budget));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

        PaymentResponse response = useCase.execute(orderId);

        assertEquals("REFUNDED", response.status());
        assertEquals(BudgetStatus.CANCELLED, budget.getStatus());
        verify(paymentGateway).refund("MOCK-1");
        verify(budgetRepository).save(budget);
    }

    @Test
    void shouldBeIdempotentWhenAlreadyRefunded() {
        Payment payment = approvedPayment();
        payment.refund();
        when(paymentRepository.findByServiceOrderId(orderId)).thenReturn(Optional.of(payment));

        PaymentResponse response = useCase.execute(orderId);

        assertEquals("REFUNDED", response.status());
        verify(paymentGateway, never()).refund(any());
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void shouldFailWhenThereIsNoPayment() {
        when(paymentRepository.findByServiceOrderId(orderId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(orderId));
    }

    @Test
    void shouldFailWhenThePaymentHasNoBudget() {
        when(paymentRepository.findByServiceOrderId(orderId)).thenReturn(Optional.of(approvedPayment()));
        when(budgetRepository.findByServiceOrderId(orderId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(orderId));
    }

    @Test
    void shouldNotRefundADeclinedPaymentNorCallTheGateway() {
        Payment declined = Payment.fromCharge(orderId, UUID.randomUUID(), Money.of(100), "credit-card",
                ChargeResult.declined("no funds"));
        when(paymentRepository.findByServiceOrderId(orderId)).thenReturn(Optional.of(declined));
        when(budgetRepository.findByServiceOrderId(orderId)).thenReturn(Optional.of(paidBudget()));

        assertThrows(InvalidStateException.class, () -> useCase.execute(orderId));
        verify(paymentGateway, never()).refund(any());
    }
}
