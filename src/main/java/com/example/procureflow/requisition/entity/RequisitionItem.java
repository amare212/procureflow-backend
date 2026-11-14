package com.example.procureflow.requisition.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "requisition_items")
public class RequisitionItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() { return id; }

    // TODO: add fields matching db/migration/V1__initial_schema.sql
}
