-- Billing service: one payment attempt per service order.

CREATE TABLE payments (
    id                 UUID          NOT NULL,
    service_order_id   UUID          NOT NULL,
    budget_id          UUID          NOT NULL,
    amount             NUMERIC(15,2) NOT NULL,
    payment_method     VARCHAR(50)   NOT NULL,
    status             VARCHAR(20)   NOT NULL,
    external_reference VARCHAR(100),
    decline_reason     VARCHAR(255),
    created_at         TIMESTAMPTZ   NOT NULL,
    updated_at         TIMESTAMPTZ   NOT NULL,
    CONSTRAINT pk_payments PRIMARY KEY (id),
    CONSTRAINT uq_payments_service_order UNIQUE (service_order_id),
    CONSTRAINT fk_payments_budget FOREIGN KEY (budget_id) REFERENCES budgets (id)
);
