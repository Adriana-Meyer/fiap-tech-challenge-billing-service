package com.fiap.workshop.billing.infrastructure.persistence.mapper;

import com.fiap.workshop.billing.domain.model.payment.Payment;
import com.fiap.workshop.billing.domain.model.payment.PaymentStatus;
import com.fiap.workshop.billing.domain.model.shared.Money;
import com.fiap.workshop.billing.infrastructure.persistence.entity.PaymentJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public Payment toDomain(PaymentJpaEntity e) {
        return new Payment(e.getId(), e.getServiceOrderId(), e.getBudgetId(), Money.of(e.getAmount()),
                e.getPaymentMethod(), PaymentStatus.valueOf(e.getStatus()), e.getExternalReference(),
                e.getDeclineReason(), e.getCreatedAt(), e.getUpdatedAt());
    }

    public PaymentJpaEntity toEntity(Payment p) {
        return new PaymentJpaEntity(p.getId(), p.getServiceOrderId(), p.getBudgetId(),
                p.getAmount().getAmount(), p.getPaymentMethod(), p.getStatus().name(),
                p.getExternalReference(), p.getDeclineReason(), p.getCreatedAt(), p.getUpdatedAt());
    }
}
