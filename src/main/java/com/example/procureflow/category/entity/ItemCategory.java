package com.example.procureflow.category.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "item_categories")
public class ItemCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() { return id; }

    // TODO: add fields matching db/migration/V1__initial_schema.sql
}
