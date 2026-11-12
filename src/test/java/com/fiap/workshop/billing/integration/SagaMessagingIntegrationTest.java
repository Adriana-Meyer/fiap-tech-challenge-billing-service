package com.fiap.workshop.billing.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiap.workshop.billing.domain.model.budget.Budget;
import com.fiap.workshop.billing.domain.model.budget.BudgetStatus;
import com.fiap.workshop.billing.domain.model.payment.PaymentStatus;
import com.fiap.workshop.billing.domain.repository.BudgetRepository;
import com.fiap.workshop.billing.domain.repository.PaymentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs the service against a real SQS and SNS (LocalStack) and a real
 * PostgreSQL: a command is sent to the {@code billing-commands} queue exactly
 * as the OS service would send it, and the answer is read from a queue
 * subscribed to the {@code saga-events} topic, as the OS service reads it.
 */
@SpringBootTest
@DisplayName("Saga messaging (integration)")
class SagaMessagingIntegrationTest {

    @ServiceConnection
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("billing_db").withUsername("billing").withPassword("billing123");

    @SuppressWarnings("resource")
    static final LocalStackContainer localstack =
            new LocalStackContainer(DockerImageName.parse("localstack/localstack:3.8.1"))
                    .withServices(LocalStackContainer.Service.SQS, LocalStackContainer.Service.SNS);

    private static final String COMMANDS_QUEUE = "billing-commands";
    private static final String DEAD_LETTERS_QUEUE = "billing-commands-dlq";
    private static final String TEST_EVENTS_QUEUE = "test-saga-events";

    private static SqsClient sqs;
    private static String topicArn;
    private static String commandsUrl;
    private static String deadLettersUrl;
    private static String eventsUrl;

    static {
        postgres.start();
        localstack.start();
        createInfrastructure();
    }

    /** What the infrastructure repositories will create: queue + DLQ, topic, and a queue subscribed to the topic. */
    private static void createInfrastructure() {
        var credentials = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey()));
        Region region = Region.of(localstack.getRegion());
        sqs = SqsClient.builder().endpointOverride(localstack.getEndpoint())
                .credentialsProvider(credentials).region(region).build();
        SnsClient sns = SnsClient.builder().endpointOverride(localstack.getEndpoint())
                .credentialsProvider(credentials).region(region).build();

        deadLettersUrl = sqs.createQueue(r -> r.queueName(DEAD_LETTERS_QUEUE)).queueUrl();
        String deadLettersArn = arnOf(deadLettersUrl);
        commandsUrl = sqs.createQueue(r -> r.queueName(COMMANDS_QUEUE).attributes(Map.of(
                QueueAttributeName.VISIBILITY_TIMEOUT, "2",
                QueueAttributeName.REDRIVE_POLICY,
                "{\"deadLetterTargetArn\":\"%s\",\"maxReceiveCount\":\"2\"}".formatted(deadLettersArn)))).queueUrl();

        topicArn = sns.createTopic(r -> r.name("saga-events")).topicArn();
        eventsUrl = sqs.createQueue(r -> r.queueName(TEST_EVENTS_QUEUE)).queueUrl();
        sns.subscribe(r -> r.topicArn(topicArn).protocol("sqs").endpoint(arnOf(eventsUrl))
                .attributes(Map.of("RawMessageDelivery", "true")));
    }

    private static String arnOf(String queueUrl) {
        return sqs.getQueueAttributes(r -> r.queueUrl(queueUrl).attributeNames(QueueAttributeName.QUEUE_ARN))
                .attributes().get(QueueAttributeName.QUEUE_ARN);
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.cloud.aws.sqs.endpoint", () -> localstack.getEndpoint().toString());
        registry.add("spring.cloud.aws.sns.endpoint", () -> localstack.getEndpoint().toString());
        registry.add("spring.cloud.aws.region.static", localstack::getRegion);
        registry.add("spring.cloud.aws.credentials.access-key", localstack::getAccessKey);
        registry.add("spring.cloud.aws.credentials.secret-key", localstack::getSecretKey);
        registry.add("app.messaging.events-topic-arn", () -> topicArn);
        registry.add("app.payment.mock.decline-above", () -> "1000.00");
    }

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private BudgetRepository budgetRepository;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private TransactionTemplate transaction;

    private final List<JsonNode> received = new ArrayList<>();

    // ---- helpers ----------------------------------------------------------

    private void send(UUID messageId, UUID sagaId, UUID orderId, String type, String payload) {
        String body = """
                {"messageId":"%s","sagaId":"%s","serviceOrderId":"%s","type":"%s","payload":%s}
                """.formatted(messageId, sagaId, orderId, type, payload);
        sqs.sendMessage(r -> r.queueUrl(commandsUrl).messageBody(body));
    }

    private void send(UUID sagaId, UUID orderId, String type, String payload) {
        send(UUID.randomUUID(), sagaId, orderId, type, payload);
    }

    private static final String TWO_ITEMS = """
            {"items":[{"description":"Labor","unitPrice":100.00,"quantity":2},
                      {"description":"Oil filter","unitPrice":35.50,"quantity":1}]}
            """;

    /** Waits for an answer about this order of this type, keeping the others it reads along the way. */
    private JsonNode awaitEvent(UUID orderId, String type) throws Exception {
        long deadline = System.currentTimeMillis() + 30_000;
        while (System.currentTimeMillis() < deadline) {
            for (JsonNode event : received) {
                if (matches(event, orderId, type)) {
                    return event;
                }
            }
            for (Message message : sqs.receiveMessage(r -> r.queueUrl(eventsUrl).waitTimeSeconds(2)
                    .maxNumberOfMessages(10)).messages()) {
                received.add(objectMapper.readTree(message.body()));
                sqs.deleteMessage(r -> r.queueUrl(eventsUrl).receiptHandle(message.receiptHandle()));
            }
        }
        throw new AssertionError("No " + type + " for " + orderId + " within 30s; received " + received);
    }

    private static boolean matches(JsonNode event, UUID orderId, String type) {
        return orderId.toString().equals(event.get("serviceOrderId").asText()) && type.equals(event.get("type").asText());
    }

    /** The adapter maps lazily loaded items, so the read needs a session: the use cases supply one, a test does not. */
    private Budget budget(UUID orderId) {
        return transaction.execute(status -> budgetRepository.findByServiceOrderId(orderId).orElseThrow());
    }

    private com.fiap.workshop.billing.domain.model.payment.Payment payment(UUID orderId) {
        return transaction.execute(status -> paymentRepository.findByServiceOrderId(orderId).orElseThrow());
    }

    private long countEvents(UUID orderId, String type) {
        return received.stream().filter(e -> matches(e, orderId, type)).count();
    }

    // ---- tests ------------------------------------------------------------

    @Test
    @DisplayName("should generate the budget and answer with its total")
    void shouldGenerateTheBudgetAndAnswerWithItsTotal() throws Exception {
        UUID sagaId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        send(sagaId, orderId, "GenerateBudget", TWO_ITEMS);

        JsonNode event = awaitEvent(orderId, "BudgetGenerated");
        assertEquals(sagaId.toString(), event.get("sagaId").asText());
        assertEquals(0, new java.math.BigDecimal("235.50").compareTo(event.get("payload").get("totalAmount").decimalValue()));
        Budget budget = budget(orderId);
        assertEquals(BudgetStatus.GENERATED, budget.getStatus());
        assertEquals(2, budget.getItems().size());
    }

    @Test
    @DisplayName("should charge an approved budget and answer approved")
    void shouldChargeAnApprovedBudgetAndAnswerApproved() throws Exception {
        UUID sagaId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        send(sagaId, orderId, "GenerateBudget", TWO_ITEMS);
        awaitEvent(orderId, "BudgetGenerated");

        send(sagaId, orderId, "ProcessPayment", "{\"paymentMethod\":\"credit-card\"}");

        awaitEvent(orderId, "PaymentApproved");
        assertEquals(PaymentStatus.APPROVED, payment(orderId).getStatus());
        assertEquals(BudgetStatus.PAID, budget(orderId).getStatus());
    }

    @Test
    @DisplayName("should answer declined, not fail, when the payment is declined")
    void shouldAnswerDeclinedNotFailWhenThePaymentIsDeclined() throws Exception {
        UUID sagaId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        send(sagaId, orderId, "GenerateBudget", TWO_ITEMS);
        awaitEvent(orderId, "BudgetGenerated");

        send(sagaId, orderId, "ProcessPayment", "{\"paymentMethod\":\"mock-decline\"}");

        JsonNode declined = awaitEvent(orderId, "PaymentDeclined");
        assertTrue(declined.get("payload").get("reason").asText().contains("mock-decline"));
        assertEquals(PaymentStatus.DECLINED, payment(orderId).getStatus());

        // the compensation of a declined payment: the saga cancels the budget
        send(sagaId, orderId, "CancelBudget", "{}");
        awaitEvent(orderId, "BudgetCancelled");
        assertEquals(BudgetStatus.CANCELLED, budget(orderId).getStatus());
    }

    @Test
    @DisplayName("should refund a paid order and confirm")
    void shouldRefundAPaidOrderAndConfirm() throws Exception {
        UUID sagaId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        send(sagaId, orderId, "GenerateBudget", TWO_ITEMS);
        send(sagaId, orderId, "ProcessPayment", "{\"paymentMethod\":\"pix\"}");
        awaitEvent(orderId, "PaymentApproved");

        send(sagaId, orderId, "RefundPayment", "{}");

        awaitEvent(orderId, "PaymentRefunded");
        assertEquals(PaymentStatus.REFUNDED, payment(orderId).getStatus());
        assertEquals(BudgetStatus.CANCELLED, budget(orderId).getStatus());
    }

    @Test
    @DisplayName("should answer a repeated command with the same message id and charge only once")
    void shouldAnswerARepeatedCommandWithTheSameMessageIdAndChargeOnlyOnce() throws Exception {
        UUID sagaId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        send(sagaId, orderId, "GenerateBudget", TWO_ITEMS);
        awaitEvent(orderId, "BudgetGenerated");

        UUID commandId = UUID.randomUUID();
        send(commandId, sagaId, orderId, "ProcessPayment", "{\"paymentMethod\":\"pix\"}");
        send(commandId, sagaId, orderId, "ProcessPayment", "{\"paymentMethod\":\"pix\"}");
        JsonNode first = awaitEvent(orderId, "PaymentApproved");
        long deadline = System.currentTimeMillis() + 15_000;
        while (countEvents(orderId, "PaymentApproved") < 2 && System.currentTimeMillis() < deadline) {
            awaitEventQuietly(orderId);
        }

        assertEquals(2, countEvents(orderId, "PaymentApproved"));
        List<String> ids = received.stream().filter(e -> matches(e, orderId, "PaymentApproved"))
                .map(e -> e.get("messageId").asText()).distinct().toList();
        assertEquals(List.of(first.get("messageId").asText()), ids);
        assertNotNull(payment(orderId).getExternalReference());
    }

    @Test
    @DisplayName("should move a message it cannot handle to the dead-letter queue")
    void shouldMoveAMessageItCannotHandleToTheDeadLetterQueue() {
        UUID orderId = UUID.randomUUID();
        // Cancelling a budget that was never generated fails in the use case on every attempt.
        send(UUID.randomUUID(), orderId, "CancelBudget", "{}");

        long deadline = System.currentTimeMillis() + 45_000;
        List<Message> dead = List.of();
        while (dead.isEmpty() && System.currentTimeMillis() < deadline) {
            dead = sqs.receiveMessage(r -> r.queueUrl(deadLettersUrl).waitTimeSeconds(2)).messages();
        }

        assertEquals(1, dead.size());
        assertTrue(dead.get(0).body().contains(orderId.toString()));
    }

    /** One short read of the events queue, only to let a late answer arrive. */
    private void awaitEventQuietly(UUID orderId) throws Exception {
        for (Message message : sqs.receiveMessage(r -> r.queueUrl(eventsUrl).waitTimeSeconds(2)
                .maxNumberOfMessages(10)).messages()) {
            received.add(objectMapper.readTree(message.body()));
            sqs.deleteMessage(r -> r.queueUrl(eventsUrl).receiptHandle(message.receiptHandle()));
        }
    }
}
