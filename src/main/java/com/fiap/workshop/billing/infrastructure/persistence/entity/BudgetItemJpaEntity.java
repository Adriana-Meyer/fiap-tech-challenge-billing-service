package com.fiap.workshop.billing.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "budget_items")
public class BudgetItemJpaEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "position", nullable = false)
    private int position;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    protected BudgetItemJpaEntity() {}

    public BudgetItemJpaEntity(UUID id, int position, String description, BigDecimal unitPrice, int quantity) {
        this.id = id;
        this.position = position;
        this.description = description;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
    }

    public UUID getId() { return id; }
    public int getPosition() { return position; }
    public String getDescription() { return description; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public int getQuantity() { return quantity; }
}
