package com.example.procureflow.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record StockAdjustmentRequest(
        @NotNull(message = "Item ID is required")
        Long itemId,

        @NotNull(message = "Adjusted quantity is required")
        @Min(value = 0, message = "Adjusted quantity must be >= 0")
        Integer adjustedQuantity,

        @NotBlank(message = "Reason is required")
        String reason
) {
}
