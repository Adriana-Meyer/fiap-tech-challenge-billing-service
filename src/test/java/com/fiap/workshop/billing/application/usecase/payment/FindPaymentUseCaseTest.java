package com.fiap.workshop.billing.application.usecase.payment;

import com.fiap.workshop.billing.application.dto.payment.PaymentResponse;
import com.fiap.workshop.billing.domain.exception.ResourceNotFoundException;
import com.fiap.workshop.billing.domain.gateway.ChargeResult;
import com.fiap.workshop.billing.domain.model.payment.Payment;
import com.fiap.workshop.billing.domain.model.shared.Money;
import com.fiap.workshop.billing.domain.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindPaymentUseCaseTest {

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private FindPaymentUseCase useCase;

    private final UUID orderId = UUID.randomUUID();

    @Test
    void shouldFindThePaymentOfAnOrder() {
        Payment payment = Payment.fromCharge(orderId, UUID.randomUUID(), Money.of(50), "pix",
                ChargeResult.approved("MOCK-9"));
        when(paymentRepository.findByServiceOrderId(orderId)).thenReturn(Optional.of(payment));

        PaymentResponse response = useCase.findByServiceOrderId(orderId);

        assertEquals("MOCK-9", response.externalReference());
    }

    @Test
    void shouldFailWhenThereIsNoPayment() {
        when(paymentRepository.findByServiceOrderId(orderId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> useCase.findByServiceOrderId(orderId));
    }
}
