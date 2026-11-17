package com.fiap.workshop.billing.interfaces.rest;

import com.fiap.workshop.billing.application.dto.budget.BudgetItemResponse;
import com.fiap.workshop.billing.application.dto.budget.BudgetResponse;
import com.fiap.workshop.billing.application.dto.payment.PaymentResponse;
import com.fiap.workshop.billing.application.port.in.budget.FindBudgetInputPort;
import com.fiap.workshop.billing.application.port.in.payment.FindPaymentInputPort;
import com.fiap.workshop.billing.domain.exception.InvalidStateException;
import com.fiap.workshop.billing.domain.exception.ResourceNotFoundException;
import com.fiap.workshop.billing.infrastructure.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({BudgetController.class, PaymentController.class})
@AutoConfigureMockMvc(addFilters = false)
class BillingControllersTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FindBudgetInputPort findBudget;

    @MockBean
    private FindPaymentInputPort findPayment;

    @MockBean
    private JwtService jwtService;

    private final UUID orderId = UUID.randomUUID();

    @Test
    void shouldReturnTheBudgetOfAnOrder() throws Exception {
        BudgetResponse response = new BudgetResponse(UUID.randomUUID(), orderId,
                List.of(new BudgetItemResponse("Labor", BigDecimal.TEN, 2, new BigDecimal("20.00"))),
                new BigDecimal("20.00"), "GENERATED", Instant.now(), Instant.now());
        when(findBudget.findByServiceOrderId(orderId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/budgets/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceOrderId").value(orderId.toString()))
                .andExpect(jsonPath("$.status").value("GENERATED"))
                .andExpect(jsonPath("$.items[0].description").value("Labor"))
                .andExpect(jsonPath("$.totalAmount").value(20.00));
    }

    @Test
    void shouldReturn404WhenTheBudgetDoesNotExist() throws Exception {
        when(findBudget.findByServiceOrderId(orderId)).thenThrow(new ResourceNotFoundException("Budget", orderId));

        mockMvc.perform(get("/api/v1/budgets/{id}", orderId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void shouldReturnThePaymentOfAnOrder() throws Exception {
        PaymentResponse response = new PaymentResponse(UUID.randomUUID(), orderId, UUID.randomUUID(),
                new BigDecimal("20.00"), "pix", "APPROVED", "MOCK-1", null, Instant.now(), Instant.now());
        when(findPayment.findByServiceOrderId(orderId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/payments/{id}", orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"))
                .andExpect(jsonPath("$.externalReference").value("MOCK-1"));
    }

    @Test
    void shouldReturn409ForAnInvalidStateTransition() throws Exception {
        when(findPayment.findByServiceOrderId(orderId)).thenThrow(new InvalidStateException("not allowed"));

        mockMvc.perform(get("/api/v1/payments/{id}", orderId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("not allowed"));
    }

    @Test
    void shouldReturn400ForAMalformedId() throws Exception {
        mockMvc.perform(get("/api/v1/budgets/not-a-uuid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn500ForUnexpectedErrors() throws Exception {
        when(findBudget.findByServiceOrderId(orderId)).thenThrow(new IllegalStateException("boom"));

        mockMvc.perform(get("/api/v1/budgets/{id}", orderId))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }
}
