package com.fiap.workshop.billing.application.usecase.payment;

import com.fiap.workshop.billing.application.dto.payment.PaymentResponse;
import com.fiap.workshop.billing.application.port.in.payment.RefundPaymentInputPort;
import com.fiap.workshop.billing.domain.exception.ResourceNotFoundException;
import com.fiap.workshop.billing.domain.gateway.PaymentGateway;
import com.fiap.workshop.billing.domain.model.budget.Budget;
import com.fiap.workshop.billing.domain.model.payment.Payment;
import com.fiap.workshop.billing.domain.model.payment.PaymentStatus;
import com.fiap.workshop.billing.domain.repository.BudgetRepository;
import com.fiap.workshop.billing.domain.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Saga compensation for an approved payment. Idempotent: refunding twice is a no-op. */
@Service
public class RefundPaymentUseCase implements RefundPaymentInputPort {

    private final BudgetRepository budgetRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentGateway paymentGateway;

    public RefundPaymentUseCase(BudgetRepository budgetRepository,
                                PaymentRepository paymentRepository,
                                PaymentGateway paymentGateway) {
        this.budgetRepository = budgetRepository;
        this.paymentRepository = paymentRepository;
        this.paymentGateway = paymentGateway;
    }

    @Transactional
    public PaymentResponse execute(UUID serviceOrderId) {
        Payment payment = paymentRepository.findByServiceOrderId(serviceOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", serviceOrderId));
        if (payment.getStatus() == PaymentStatus.REFUNDED) {
            return PaymentResponse.from(payment);
        }

        Budget budget = budgetRepository.findByServiceOrderId(serviceOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Budget", serviceOrderId));

        payment.refund();
        paymentGateway.refund(payment.getExternalReference());
        budget.cancelAfterRefund();

        budgetRepository.save(budget);
        return PaymentResponse.from(paymentRepository.save(payment));
    }
}
