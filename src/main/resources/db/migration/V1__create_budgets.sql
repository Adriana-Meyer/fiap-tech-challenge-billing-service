-- Billing service: one budget per service order, with a snapshot of its items.

CREATE TABLE budgets (
    id               UUID          NOT NULL,
    service_order_id UUID          NOT NULL,
    status           VARCHAR(20)   NOT NULL,
    total_amount     NUMERIC(15,2) NOT NULL,
    created_at       TIMESTAMPTZ   NOT NULL,
    updated_at       TIMESTAMPTZ   NOT NULL,
    CONSTRAINT pk_budgets PRIMARY KEY (id),
    CONSTRAINT uq_budgets_service_order UNIQUE (service_order_id)
);

CREATE TABLE budget_items (
    id          UUID          NOT NULL,
    budget_id   UUID          NOT NULL,
    position    INT           NOT NULL,
    description VARCHAR(255)  NOT NULL,
    unit_price  NUMERIC(15,2) NOT NULL,
    quantity    INT           NOT NULL,
    CONSTRAINT pk_budget_items PRIMARY KEY (id),
    CONSTRAINT fk_budget_items_budget FOREIGN KEY (budget_id) REFERENCES budgets (id) ON DELETE CASCADE
);

CREATE INDEX idx_budget_items_budget ON budget_items (budget_id);
