package com.fiap.workshop.billing.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiap.workshop.billing.application.saga.event.BudgetGenerated;
import com.fiap.workshop.billing.application.saga.event.PaymentDeclined;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class SnsSagaEventPublisherTest {

    private static final String TOPIC = "arn:aws:sns:us-east-1:000000000000:saga-events";

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final SnsClient snsClient = mock(SnsClient.class);
    private final UUID messageId = UUID.randomUUID();
    private final UUID sagaId = UUID.randomUUID();
    private final UUID orderId = UUID.randomUUID();

    private PublishRequest sent() {
        ArgumentCaptor<PublishRequest> captor = ArgumentCaptor.forClass(PublishRequest.class);
        verify(snsClient).publish(captor.capture());
        return captor.getValue();
    }

    @Test
    void shouldPublishTheEnvelopeWithOnlyTheEventFieldsInThePayload() throws Exception {
        new SnsSagaEventPublisher(snsClient, objectMapper, TOPIC)
                .publish(new BudgetGenerated(messageId, sagaId, orderId, new BigDecimal("235.50")));

        PublishRequest request = sent();
        JsonNode body = objectMapper.readTree(request.message());
        assertEquals(TOPIC, request.topicArn());
        assertEquals(messageId.toString(), body.get("messageId").asText());
        assertEquals(sagaId.toString(), body.get("sagaId").asText());
        assertEquals(orderId.toString(), body.get("serviceOrderId").asText());
        assertEquals("BudgetGenerated", body.get("type").asText());
        assertEquals(0, new BigDecimal("235.50").compareTo(body.get("payload").get("totalAmount").decimalValue()));
        assertFalse(body.get("payload").has("sagaId"));
        assertFalse(body.get("payload").has("messageId"));
        assertEquals("BudgetGenerated", request.messageAttributes().get("type").stringValue());
    }

    @Test
    void shouldCarryTheReasonOfADecline() throws Exception {
        new SnsSagaEventPublisher(snsClient, objectMapper, TOPIC)
                .publish(new PaymentDeclined(messageId, sagaId, orderId, "no funds"));

        assertEquals("no funds", objectMapper.readTree(sent().message()).get("payload").get("reason").asText());
    }

    @Test
    void shouldFailLoudlyWhenTheTopicIsNotConfigured() {
        SnsSagaEventPublisher publisher = new SnsSagaEventPublisher(snsClient, objectMapper, "");

        assertThrows(IllegalStateException.class,
                () -> publisher.publish(new PaymentDeclined(messageId, sagaId, orderId, "x")));
        verifyNoInteractions(snsClient);
    }
}
