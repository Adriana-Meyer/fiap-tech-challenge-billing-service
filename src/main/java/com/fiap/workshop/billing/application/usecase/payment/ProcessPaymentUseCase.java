package com.fiap.workshop.billing.application.usecase.payment;

import com.fiap.workshop.billing.application.dto.payment.PaymentResponse;
import com.fiap.workshop.billing.application.dto.payment.ProcessPaymentCommand;
import com.fiap.workshop.billing.application.port.in.payment.ProcessPaymentInputPort;
import com.fiap.workshop.billing.domain.exception.ResourceNotFoundException;
import com.fiap.workshop.billing.domain.gateway.ChargeResult;
import com.fiap.workshop.billing.domain.gateway.PaymentGateway;
import com.fiap.workshop.billing.domain.model.budget.Budget;
import com.fiap.workshop.billing.domain.model.payment.Payment;
import com.fiap.workshop.billing.domain.repository.BudgetRepository;
import com.fiap.workshop.billing.domain.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Charges the budget of a service order. Idempotent: a payment that already
 * exists (approved or declined) is returned as is, so a redelivered command
 * never charges twice. A decline is returned as a DECLINED payment, not thrown,
 * because the saga reacts to it with compensations.
 */
@Service
public class ProcessPaymentUseCase implements ProcessPaymentInputPort {

    private final BudgetRepository budgetRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentGateway paymentGateway;

    public ProcessPaymentUseCase(BudgetRepository budgetRepository,
                                 PaymentRepository paymentRepository,
                                 PaymentGateway paymentGateway) {
        this.budgetRepository = budgetRepository;
        this.paymentRepository = paymentRepository;
        this.paymentGateway = paymentGateway;
    }

    @Transactional
    public PaymentResponse execute(ProcessPaymentCommand command) {
        return paymentRepository.findByServiceOrderId(command.serviceOrderId())
                .map(PaymentResponse::from)
                .orElseGet(() -> charge(command));
    }

    private PaymentResponse charge(ProcessPaymentCommand command) {
        Budget budget = budgetRepository.findByServiceOrderId(command.serviceOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Budget", command.serviceOrderId()));
        budget.ensurePayable();

        ChargeResult result = paymentGateway.charge(
                budget.getServiceOrderId(), budget.getTotalAmount(), command.paymentMethod());
        Payment payment = Payment.fromCharge(
                budget.getServiceOrderId(), budget.getId(), budget.getTotalAmount(),
                command.paymentMethod(), result);

        if (result.approved()) {
            budget.markPaid();
            budgetRepository.save(budget);
        }
        return PaymentResponse.from(paymentRepository.save(payment));
    }
}
