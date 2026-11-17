package com.fiap.workshop.billing.application.usecase.saga;

import com.fiap.workshop.billing.application.dto.budget.BudgetItemCommand;
import com.fiap.workshop.billing.application.dto.budget.BudgetResponse;
import com.fiap.workshop.billing.application.dto.budget.GenerateBudgetCommand;
import com.fiap.workshop.billing.application.dto.payment.PaymentResponse;
import com.fiap.workshop.billing.application.dto.payment.ProcessPaymentCommand;
import com.fiap.workshop.billing.application.port.in.budget.CancelBudgetInputPort;
import com.fiap.workshop.billing.application.port.in.budget.GenerateBudgetInputPort;
import com.fiap.workshop.billing.application.port.in.payment.ProcessPaymentInputPort;
import com.fiap.workshop.billing.application.port.in.payment.RefundPaymentInputPort;
import com.fiap.workshop.billing.application.port.in.saga.HandleSagaCommandInputPort;
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
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Turns each saga command into the use case that does the work and publishes
 * the matching answer. A payment the gateway declines is an answer
 * ({@link PaymentDeclined}), not an error; anything unexpected is thrown so
 * the message goes back to the queue and, in the end, to the dead-letter queue.
 */
@Service
public class HandleSagaCommandUseCase implements HandleSagaCommandInputPort {

    private final GenerateBudgetInputPort generateBudget;
    private final ProcessPaymentInputPort processPayment;
    private final CancelBudgetInputPort cancelBudget;
    private final RefundPaymentInputPort refundPayment;
    private final SagaEventPublisher publisher;

    public HandleSagaCommandUseCase(GenerateBudgetInputPort generateBudget,
                                    ProcessPaymentInputPort processPayment,
                                    CancelBudgetInputPort cancelBudget,
                                    RefundPaymentInputPort refundPayment,
                                    SagaEventPublisher publisher) {
        this.generateBudget = generateBudget;
        this.processPayment = processPayment;
        this.cancelBudget = cancelBudget;
        this.refundPayment = refundPayment;
        this.publisher = publisher;
    }

    @Override
    public void handle(UUID messageId, SagaCommand command) {
        if (command instanceof GenerateBudget c) {
            onGenerateBudget(messageId, c);
        } else if (command instanceof ProcessPayment c) {
            onProcessPayment(messageId, c);
        } else if (command instanceof CancelBudget c) {
            cancelBudget.execute(c.serviceOrderId());
            publisher.publish(new BudgetCancelled(answerId(messageId, BudgetCancelled.class),
                    c.sagaId(), c.serviceOrderId()));
        } else if (command instanceof RefundPayment c) {
            refundPayment.execute(c.serviceOrderId());
            publisher.publish(new PaymentRefunded(answerId(messageId, PaymentRefunded.class),
                    c.sagaId(), c.serviceOrderId()));
        } else {
            throw new IllegalArgumentException("Unsupported saga command " + command.getClass().getName());
        }
    }

    private void onGenerateBudget(UUID messageId, GenerateBudget command) {
        BudgetResponse budget = generateBudget.execute(new GenerateBudgetCommand(command.serviceOrderId(),
                command.items().stream()
                        .map(line -> new BudgetItemCommand(line.description(), line.unitPrice(), line.quantity()))
                        .toList()));
        publisher.publish(new BudgetGenerated(answerId(messageId, BudgetGenerated.class),
                command.sagaId(), command.serviceOrderId(), budget.totalAmount()));
    }

    private void onProcessPayment(UUID messageId, ProcessPayment command) {
        PaymentResponse payment = processPayment.execute(
                new ProcessPaymentCommand(command.serviceOrderId(), command.paymentMethod()));
        if ("DECLINED".equals(payment.status())) {
            publisher.publish(new PaymentDeclined(answerId(messageId, PaymentDeclined.class),
                    command.sagaId(), command.serviceOrderId(), payment.declineReason()));
        } else {
            // APPROVED, or REFUNDED when this is a late repeat of a command whose payment was since undone.
            publisher.publish(new PaymentApproved(answerId(messageId, PaymentApproved.class),
                    command.sagaId(), command.serviceOrderId()));
        }
    }

    /** Same command message, same answer id: the orchestrator recognizes a repeat. */
    private static UUID answerId(UUID commandMessageId, Class<? extends SagaEvent> eventType) {
        return UUID.nameUUIDFromBytes(
                (commandMessageId + ":" + eventType.getSimpleName()).getBytes(StandardCharsets.UTF_8));
    }
}
