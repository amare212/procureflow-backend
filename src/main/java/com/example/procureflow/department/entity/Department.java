package com.example.procureflow.department.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "departments")
public class Department {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    public Long getId() { return id; }

    // TODO: add fields matching db/migration/V1__initial_schema.sql
}
