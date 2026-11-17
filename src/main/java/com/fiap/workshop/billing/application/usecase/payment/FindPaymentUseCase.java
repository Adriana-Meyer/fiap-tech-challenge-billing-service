package com.fiap.workshop.billing.application.usecase.payment;

import com.fiap.workshop.billing.application.dto.payment.PaymentResponse;
import com.fiap.workshop.billing.application.port.in.payment.FindPaymentInputPort;
import com.fiap.workshop.billing.domain.exception.ResourceNotFoundException;
import com.fiap.workshop.billing.domain.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class FindPaymentUseCase implements FindPaymentInputPort {

    private final PaymentRepository paymentRepository;

    public FindPaymentUseCase(PaymentRepository paymentRepository) {
        this.paymentRepository = paymentRepository;
    }

    @Transactional(readOnly = true)
    public PaymentResponse findByServiceOrderId(UUID serviceOrderId) {
        return paymentRepository.findByServiceOrderId(serviceOrderId)
                .map(PaymentResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", serviceOrderId));
    }
}
