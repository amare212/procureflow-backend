package com.example.procureflow.stockmovement.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "stock_movements")
public class StockMovement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() { return id; }

    // TODO: add fields matching db/migration/V1__initial_schema.sql
}
