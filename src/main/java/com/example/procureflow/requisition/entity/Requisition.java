package com.example.procureflow.requisition.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "requisitions")
public class Requisition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() { return id; }

    // TODO: add fields matching db/migration/V1__initial_schema.sql
}
