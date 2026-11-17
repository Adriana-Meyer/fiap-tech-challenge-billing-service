package com.fiap.workshop.billing.interfaces.rest;

import com.fiap.workshop.billing.application.dto.payment.PaymentResponse;
import com.fiap.workshop.billing.application.port.in.payment.FindPaymentInputPort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** Read-only: payments are charged and refunded by saga commands, never by HTTP. */
@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final FindPaymentInputPort findUseCase;

    public PaymentController(FindPaymentInputPort findUseCase) {
        this.findUseCase = findUseCase;
    }

    @GetMapping("/{serviceOrderId}")
    public PaymentResponse findByServiceOrderId(@PathVariable UUID serviceOrderId) {
        return findUseCase.findByServiceOrderId(serviceOrderId);
    }
}
