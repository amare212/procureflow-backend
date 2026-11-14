package com.example.procureflow.department.entity;

import com.example.procureflow.common.entity.BaseEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "departments")
public class Department extends BaseEntity {

    @Column(nullable = false, unique = true, length = 120)
    private String name;

    @Column(length = 20)
    private String code;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal budget = BigDecimal.ZERO;

    public Department() {
    }

    public Department(String name, String code, BigDecimal budget) {
        this.name = name;
        this.code = code;
        this.budget = budget != null ? budget : BigDecimal.ZERO;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public BigDecimal getBudget() {
        return budget;
    }

    public void setBudget(BigDecimal budget) {
        this.budget = budget;
    }
}
