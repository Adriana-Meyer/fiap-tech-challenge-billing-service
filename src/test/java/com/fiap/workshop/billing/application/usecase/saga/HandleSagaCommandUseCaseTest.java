package com.fiap.workshop.billing.application.usecase.saga;

import com.fiap.workshop.billing.application.dto.budget.BudgetResponse;
import com.fiap.workshop.billing.application.dto.payment.PaymentResponse;
import com.fiap.workshop.billing.application.port.in.budget.CancelBudgetInputPort;
import com.fiap.workshop.billing.application.port.in.budget.GenerateBudgetInputPort;
import com.fiap.workshop.billing.application.port.in.payment.ProcessPaymentInputPort;
import com.fiap.workshop.billing.application.port.in.payment.RefundPaymentInputPort;
import com.fiap.workshop.billing.application.port.out.SagaEventPublisher;
import com.fiap.workshop.billing.application.saga.command.CancelBudget;
import com.fiap.workshop.billing.application.saga.command.GenerateBudget;
import com.fiap.workshop.billing.application.saga.command.ProcessPayment;
import com.fiap.workshop.billing.application.saga.command.RefundPayment;
import com.fiap.workshop.billing.application.saga.command.SagaCommand;
import com.fiap.workshop.billing.application.saga.event.BudgetCancelled;
import com.fiap.workshop.billing.application.saga.event.BudgetGenerated;
import com.fiap.workshop.billing.application.saga.event.PaymentApproved;
import com.fiap.workshop.billing.application.saga.event.PaymentDeclined;
import com.fiap.workshop.billing.application.saga.event.PaymentRefunded;
import com.fiap.workshop.billing.application.saga.event.SagaEvent;
import com.fiap.workshop.billing.domain.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HandleSagaCommandUseCaseTest {

    @Mock
    private GenerateBudgetInputPort generateBudget;
    @Mock
    private ProcessPaymentInputPort processPayment;
    @Mock
    private CancelBudgetInputPort cancelBudget;
    @Mock
    private RefundPaymentInputPort refundPayment;
    @Mock
    private SagaEventPublisher publisher;

    private HandleSagaCommandUseCase useCase;

    private final UUID messageId = UUID.randomUUID();
    private final UUID sagaId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        useCase = new HandleSagaCommandUseCase(generateBudget, processPayment, cancelBudget, refundPayment, publisher);
    }

    private SagaEvent published() {
        ArgumentCaptor<SagaEvent> captor = ArgumentCaptor.forClass(SagaEvent.class);
        verify(publisher).publish(captor.capture());
        return captor.getValue();
    }

    private PaymentResponse payment(String status, String declineReason) {
        return new PaymentResponse(UUID.randomUUID(), orderId, UUID.randomUUID(), BigDecimal.TEN, "pix",
                status, "MOCK-1", declineReason, Instant.now(), Instant.now());
    }

    @Test
    void shouldGenerateTheBudgetAndAnswerWithItsTotal() {
        BudgetResponse budget = new BudgetResponse(UUID.randomUUID(), orderId, List.of(),
                new BigDecimal("235.50"), "GENERATED", Instant.now(), Instant.now());
        when(generateBudget.execute(any())).thenReturn(budget);

        useCase.handle(messageId, new GenerateBudget(sagaId, orderId,
                List.of(new GenerateBudget.Line("Labor", new BigDecimal("100.00"), 2))));

        BudgetGenerated event = (BudgetGenerated) published();
        assertEquals(new BigDecimal("235.50"), event.totalAmount());
        assertEquals(sagaId, event.sagaId());
        assertEquals(orderId, event.serviceOrderId());
    }

    @Test
    void shouldAnswerApprovedWhenThePaymentIsApproved() {
        when(processPayment.execute(any())).thenReturn(payment("APPROVED", null));

        useCase.handle(messageId, new ProcessPayment(sagaId, orderId, "pix"));

        assertEquals(PaymentApproved.class, published().getClass());
    }

    @Test
    void shouldAnswerDeclinedWithTheReasonInsteadOfFailing() {
        when(processPayment.execute(any())).thenReturn(payment("DECLINED", "Amount above the limit"));

        useCase.handle(messageId, new ProcessPayment(sagaId, orderId, "pix"));

        PaymentDeclined event = (PaymentDeclined) published();
        assertEquals("Amount above the limit", event.reason());
    }

    @Test
    void shouldAnswerApprovedForALateRepeatOfARefundedPayment() {
        when(processPayment.execute(any())).thenReturn(payment("REFUNDED", null));

        useCase.handle(messageId, new ProcessPayment(sagaId, orderId, "pix"));

        assertEquals(PaymentApproved.class, published().getClass());
    }

    @Test
    void shouldCancelTheBudgetAndConfirm() {
        useCase.handle(messageId, new CancelBudget(sagaId, orderId));

        verify(cancelBudget).execute(orderId);
        assertEquals(BudgetCancelled.class, published().getClass());
    }

    @Test
    void shouldRefundThePaymentAndConfirm() {
        useCase.handle(messageId, new RefundPayment(sagaId, orderId));

        verify(refundPayment).execute(orderId);
        assertEquals(PaymentRefunded.class, published().getClass());
    }

    @Test
    void shouldDeriveTheAnswerIdFromTheCommandMessageId() {
        useCase.handle(messageId, new CancelBudget(sagaId, orderId));
        SagaEvent first = published();

        org.mockito.Mockito.clearInvocations(publisher);
        useCase.handle(messageId, new CancelBudget(sagaId, orderId));
        SagaEvent repeat = published();
        org.mockito.Mockito.clearInvocations(publisher);
        useCase.handle(UUID.randomUUID(), new CancelBudget(sagaId, orderId));
        SagaEvent other = published();

        assertEquals(first.messageId(), repeat.messageId());
        assertNotEquals(first.messageId(), other.messageId());
    }

    @Test
    void shouldNotAnswerWhenTheUseCaseFails() {
        org.mockito.Mockito.doThrow(new ResourceNotFoundException("Budget", orderId))
                .when(cancelBudget).execute(orderId);

        assertThrows(ResourceNotFoundException.class,
                () -> useCase.handle(messageId, new CancelBudget(sagaId, orderId)));
        verify(publisher, never()).publish(any());
    }

    @Test
    void shouldRejectACommandItDoesNotKnow() {
        SagaCommand unknown = new SagaCommand() {
            public UUID sagaId() { return sagaId; }
            public UUID serviceOrderId() { return orderId; }
        };

        assertThrows(IllegalArgumentException.class, () -> useCase.handle(messageId, unknown));
        verifyNoInteractions(generateBudget, processPayment, cancelBudget, refundPayment, publisher);
    }
}
