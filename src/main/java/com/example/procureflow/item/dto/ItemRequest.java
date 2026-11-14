package com.example.procureflow.item.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ItemRequest(
        @NotBlank(message = "SKU is required")
        @Size(max = 60, message = "SKU must not exceed 60 characters")
        String sku,

        @NotBlank(message = "Name is required")
        @Size(max = 150, message = "Name must not exceed 150 characters")
        String name,

        String description,

        @NotNull(message = "Category ID is required")
        Long categoryId,

        @NotBlank(message = "Unit is required")
        @Size(max = 30, message = "Unit must not exceed 30 characters")
        String unit,

        @NotNull(message = "Unit price is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Unit price must be >= 0")
        BigDecimal unitPrice,

        @Min(value = 0, message = "Reorder level must be >= 0")
        Integer reorderLevel,

        Boolean active
) {
}
