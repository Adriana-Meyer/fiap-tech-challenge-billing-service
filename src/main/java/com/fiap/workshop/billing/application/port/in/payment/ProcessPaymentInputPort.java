package com.fiap.workshop.billing.application.port.in.payment;

import com.fiap.workshop.billing.application.dto.payment.PaymentResponse;
import com.fiap.workshop.billing.application.dto.payment.ProcessPaymentCommand;

public interface ProcessPaymentInputPort {
    PaymentResponse execute(ProcessPaymentCommand command);
}
