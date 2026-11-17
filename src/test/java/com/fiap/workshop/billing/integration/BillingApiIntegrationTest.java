package com.fiap.workshop.billing.integration;

import com.fiap.workshop.billing.application.dto.budget.BudgetItemCommand;
import com.fiap.workshop.billing.application.dto.budget.BudgetResponse;
import com.fiap.workshop.billing.application.dto.budget.GenerateBudgetCommand;
import com.fiap.workshop.billing.application.dto.payment.PaymentResponse;
import com.fiap.workshop.billing.application.dto.payment.ProcessPaymentCommand;
import com.fiap.workshop.billing.application.port.in.budget.CancelBudgetInputPort;
import com.fiap.workshop.billing.application.port.in.budget.GenerateBudgetInputPort;
import com.fiap.workshop.billing.application.port.in.payment.ProcessPaymentInputPort;
import com.fiap.workshop.billing.application.port.in.payment.RefundPaymentInputPort;
import com.fiap.workshop.billing.domain.exception.InvalidStateException;
import com.fiap.workshop.billing.infrastructure.security.JwtTestTokens;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Boots the whole service against a real PostgreSQL (Flyway migrations included).
 * The commands are driven through the input ports, as the saga handlers will do;
 * the read side goes through HTTP with security enabled.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.jwt.secret=" + JwtTestTokens.SECRET,
        "app.payment.mock.decline-above=1000.00",
        "app.messaging.enabled=false"
})
@DisplayName("Billing API (integration)")
class BillingApiIntegrationTest {

    @ServiceConnection
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("billing_db")
            .withUsername("billing")
            .withPassword("billing123");

    static {
        postgres.start();
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GenerateBudgetInputPort generateBudget;

    @Autowired
    private CancelBudgetInputPort cancelBudget;

    @Autowired
    private ProcessPaymentInputPort processPayment;

    @Autowired
    private RefundPaymentInputPort refundPayment;

    private static String bearer(String role) {
        return "Bearer " + JwtTestTokens.token("user-" + role, role);
    }

    private BudgetResponse generate(UUID orderId, double price, int quantity) {
        return generateBudget.execute(new GenerateBudgetCommand(orderId,
                List.of(new BudgetItemCommand("Labor", BigDecimal.valueOf(price), quantity),
                        new BudgetItemCommand("Oil filter", new BigDecimal("35.50"), 1))));
    }

    @Test
    @DisplayName("should expose health and OpenAPI without a token")
    void shouldExposeHealthAndOpenApiWithoutAToken() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("should answer 401 without a token and 403 for a role that cannot read billing data")
    void shouldProtectTheReadEndpoints() throws Exception {
        UUID orderId = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/budgets/{id}", orderId)).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/budgets/{id}", orderId)
                        .header("Authorization", "Bearer " + JwtTestTokens.token("u", null)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/budgets/{id}", orderId).header("Authorization", bearer("MECHANIC")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/payments/{id}", orderId).header("Authorization", bearer("MECHANIC")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("should answer 404 when the order has no budget or payment")
    void shouldAnswer404ForUnknownOrders() throws Exception {
        UUID orderId = UUID.randomUUID();
        mockMvc.perform(get("/api/v1/budgets/{id}", orderId).header("Authorization", bearer("CONSULTANT")))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/payments/{id}", orderId).header("Authorization", bearer("ADMIN")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("should generate a budget, charge it and read both back")
    void shouldGenerateAndPay() throws Exception {
        UUID orderId = UUID.randomUUID();
        BudgetResponse budget = generate(orderId, 100, 2);
        assertEquals(0, new BigDecimal("235.50").compareTo(budget.totalAmount()));

        PaymentResponse payment = processPayment.execute(new ProcessPaymentCommand(orderId, "credit-card"));
        assertEquals("APPROVED", payment.status());

        mockMvc.perform(get("/api/v1/budgets/{id}", orderId).header("Authorization", bearer("CONSULTANT")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.totalAmount").value(235.50))
                .andExpect(jsonPath("$.items[0].description").value("Labor"))
                .andExpect(jsonPath("$.items[1].description").value("Oil filter"));
        mockMvc.perform(get("/api/v1/payments/{id}", orderId).header("Authorization", bearer("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.amount").value(235.50));
    }

    @Test
    @DisplayName("should not charge twice when the payment command is redelivered")
    void shouldNotChargeTwice() {
        UUID orderId = UUID.randomUUID();
        generate(orderId, 10, 1);

        PaymentResponse first = processPayment.execute(new ProcessPaymentCommand(orderId, "pix"));
        PaymentResponse second = processPayment.execute(new ProcessPaymentCommand(orderId, "pix"));

        assertEquals(first.id(), second.id());
        assertEquals(first.externalReference(), second.externalReference());
    }

    @Test
    @DisplayName("should keep a declined payment and let the saga cancel the budget")
    void shouldDeclineAndCancel() throws Exception {
        UUID orderId = UUID.randomUUID();
        generate(orderId, 100, 20);

        PaymentResponse payment = processPayment.execute(new ProcessPaymentCommand(orderId, "credit-card"));
        assertEquals("DECLINED", payment.status());

        BudgetResponse cancelled = cancelBudget.execute(orderId);
        assertEquals("CANCELLED", cancelled.status());

        mockMvc.perform(get("/api/v1/payments/{id}", orderId).header("Authorization", bearer("ADMIN")))
                .andExpect(jsonPath("$.status").value("DECLINED"))
                .andExpect(jsonPath("$.declineReason").exists());
    }

    @Test
    @DisplayName("should decline the mock-decline method regardless of the amount")
    void shouldDeclineTheMockDeclineMethod() {
        UUID orderId = UUID.randomUUID();
        generate(orderId, 1, 1);

        assertEquals("DECLINED",
                processPayment.execute(new ProcessPaymentCommand(orderId, "mock-decline")).status());
    }

    @Test
    @DisplayName("should refund a paid order, cancelling its budget, and refuse to cancel a paid budget")
    void shouldRefundAPaidOrder() throws Exception {
        UUID orderId = UUID.randomUUID();
        generate(orderId, 50, 1);
        processPayment.execute(new ProcessPaymentCommand(orderId, "credit-card"));

        assertThrows(InvalidStateException.class, () -> cancelBudget.execute(orderId));

        assertEquals("REFUNDED", refundPayment.execute(orderId).status());
        assertEquals("REFUNDED", refundPayment.execute(orderId).status());

        mockMvc.perform(get("/api/v1/budgets/{id}", orderId).header("Authorization", bearer("ADMIN")))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("should replace the items when the budget is generated again")
    void shouldReplaceItemsOnRegeneration() throws Exception {
        UUID orderId = UUID.randomUUID();
        generate(orderId, 100, 2);

        BudgetResponse second = generateBudget.execute(new GenerateBudgetCommand(orderId,
                List.of(new BudgetItemCommand("Brake pads", new BigDecimal("80.00"), 1))));

        assertEquals(1, second.items().size());
        mockMvc.perform(get("/api/v1/budgets/{id}", orderId).header("Authorization", bearer("CONSULTANT")))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].description").value("Brake pads"))
                .andExpect(jsonPath("$.totalAmount").value(80.00));
    }
}
