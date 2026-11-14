package com.example.procureflow.department.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record DepartmentRequest(
        @NotBlank(message = "Department name is required")
        @Size(max = 120, message = "Name must not exceed 120 characters")
        String name,

        @Size(max = 20, message = "Code must not exceed 20 characters")
        String code,

        @DecimalMin(value = "0.0", inclusive = true, message = "Budget must be greater than or equal to 0")
        BigDecimal budget
) {
}
