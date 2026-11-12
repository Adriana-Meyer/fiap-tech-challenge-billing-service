package com.fiap.workshop.billing.infrastructure.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiap.workshop.billing.application.port.in.saga.HandleSagaCommandInputPort;
import com.fiap.workshop.billing.application.saga.command.CancelBudget;
import com.fiap.workshop.billing.application.saga.command.GenerateBudget;
import com.fiap.workshop.billing.application.saga.command.ProcessPayment;
import com.fiap.workshop.billing.application.saga.command.RefundPayment;
import com.fiap.workshop.billing.application.saga.command.SagaCommand;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class SagaCommandListenerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final HandleSagaCommandInputPort handler = mock(HandleSagaCommandInputPort.class);
    private final SagaCommandListener listener = new SagaCommandListener(objectMapper, handler);

    private final UUID messageId = UUID.randomUUID();
    private final UUID sagaId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();

    private String envelope(String type, String payload) {
        return """
                {"messageId":"%s","sagaId":"%s","serviceOrderId":"%s","type":"%s","payload":%s}
                """.formatted(messageId, sagaId, orderId, type, payload);
    }

    private SagaCommand handled() {
        ArgumentCaptor<SagaCommand> captor = ArgumentCaptor.forClass(SagaCommand.class);
        verify(handler).handle(org.mockito.ArgumentMatchers.eq(messageId), captor.capture());
        return captor.getValue();
    }

    @Test
    void shouldHandleGenerateBudgetMergingTheEnvelopeIds() throws Exception {
        listener.onMessage(envelope("GenerateBudget",
                "{\"items\":[{\"description\":\"Labor\",\"unitPrice\":100.00,\"quantity\":2}]}"));

        GenerateBudget command = (GenerateBudget) handled();
        assertEquals(sagaId, command.sagaId());
        assertEquals(orderId, command.serviceOrderId());
        assertEquals("Labor", command.items().get(0).description());
        assertEquals(0, new BigDecimal("100.00").compareTo(command.items().get(0).unitPrice()));
        assertEquals(2, command.items().get(0).quantity());
    }

    @Test
    void shouldHandleProcessPayment() throws Exception {
        listener.onMessage(envelope("ProcessPayment", "{\"paymentMethod\":\"pix\"}"));

        assertEquals("pix", ((ProcessPayment) handled()).paymentMethod());
    }

    @Test
    void shouldHandleTheCompensationsWithAnEmptyOrMissingPayload() throws Exception {
        listener.onMessage(envelope("CancelBudget", "{}"));
        assertEquals(CancelBudget.class, handled().getClass());
        org.mockito.Mockito.clearInvocations(handler);

        listener.onMessage(envelope("RefundPayment", "null"));
        assertEquals(RefundPayment.class, handled().getClass());
    }

    @Test
    void shouldFailOnAnUnknownTypeSoTheMessageGoesToTheDeadLetterQueue() {
        assertThrows(IllegalArgumentException.class, () -> listener.onMessage(envelope("ReserveStock", "{}")));
        verifyNoInteractions(handler);
    }

    @Test
    void shouldFailOnAMessageThatIsNotJson() {
        assertThrows(JsonProcessingException.class, () -> listener.onMessage("not json"));
        verifyNoInteractions(handler);
    }

    @Test
    void shouldFailWhenTheHandlerFailsSoTheMessageIsRetried() {
        org.mockito.Mockito.doThrow(new IllegalStateException("db down")).when(handler).handle(any(), any());

        assertThrows(IllegalStateException.class, () -> listener.onMessage(envelope("CancelBudget", "{}")));
    }
}
