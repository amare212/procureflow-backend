package com.example.procureflow.department.dto;

import com.example.procureflow.department.entity.Department;

import java.math.BigDecimal;
import java.time.Instant;

public record DepartmentResponse(
        Long id,
        String name,
        String code,
        BigDecimal budget,
        Instant createdAt,
        Instant updatedAt
) {
    public static DepartmentResponse fromEntity(Department dept) {
        return new DepartmentResponse(
                dept.getId(),
                dept.getName(),
                dept.getCode(),
                dept.getBudget(),
                dept.getCreatedAt(),
                dept.getUpdatedAt()
        );
    }
}
