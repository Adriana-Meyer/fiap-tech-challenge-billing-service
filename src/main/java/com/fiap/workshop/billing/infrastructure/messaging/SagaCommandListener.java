package com.fiap.workshop.billing.infrastructure.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fiap.workshop.billing.application.port.in.saga.HandleSagaCommandInputPort;
import com.fiap.workshop.billing.application.saga.command.CancelBudget;
import com.fiap.workshop.billing.application.saga.command.GenerateBudget;
import com.fiap.workshop.billing.application.saga.command.ProcessPayment;
import com.fiap.workshop.billing.application.saga.command.RefundPayment;
import com.fiap.workshop.billing.application.saga.command.SagaCommand;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Reads the commands the OS service sends to the {@code billing-commands} queue.
 * A message that fails (bad JSON, unknown type, a use case that throws) is not
 * acknowledged: SQS delivers it again and, after the queue's maximum receive
 * count, moves it to the dead-letter queue.
 */
@Component
@ConditionalOnProperty(name = "app.messaging.enabled", havingValue = "true", matchIfMissing = true)
public class SagaCommandListener {

    private static final Logger log = LoggerFactory.getLogger(SagaCommandListener.class);

    private final ObjectMapper objectMapper;
    private final HandleSagaCommandInputPort handler;

    public SagaCommandListener(ObjectMapper objectMapper, HandleSagaCommandInputPort handler) {
        this.objectMapper = objectMapper;
        this.handler = handler;
    }

    @SqsListener("${app.messaging.commands-queue}")
    public void onMessage(String body) throws JsonProcessingException {
        MessageEnvelope envelope = objectMapper.readValue(body, MessageEnvelope.class);
        MDC.put("sagaId", String.valueOf(envelope.sagaId()));
        try {
            log.info("Received {} for service order {}", envelope.type(), envelope.serviceOrderId());
            handler.handle(envelope.messageId(), toCommand(envelope));
        } finally {
            MDC.remove("sagaId");
        }
    }

    private SagaCommand toCommand(MessageEnvelope envelope) throws JsonProcessingException {
        Class<? extends SagaCommand> type = switch (String.valueOf(envelope.type())) {
            case "GenerateBudget" -> GenerateBudget.class;
            case "ProcessPayment" -> ProcessPayment.class;
            case "CancelBudget" -> CancelBudget.class;
            case "RefundPayment" -> RefundPayment.class;
            default -> throw new IllegalArgumentException("Unknown saga command type: " + envelope.type());
        };
        ObjectNode fields = envelope.payload() instanceof ObjectNode payload
                ? payload.deepCopy() : objectMapper.createObjectNode();
        fields.put("sagaId", String.valueOf(envelope.sagaId()));
        fields.put("serviceOrderId", String.valueOf(envelope.serviceOrderId()));
        return objectMapper.treeToValue(fields, type);
    }
}
