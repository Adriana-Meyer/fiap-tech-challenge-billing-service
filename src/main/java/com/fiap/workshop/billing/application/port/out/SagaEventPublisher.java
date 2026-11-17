package com.fiap.workshop.billing.application.port.out;

import com.fiap.workshop.billing.application.saga.event.SagaEvent;

/** Sends an answer to the OS service. A failure must be thrown so the command is retried. */
public interface SagaEventPublisher {

    void publish(SagaEvent event);
}
