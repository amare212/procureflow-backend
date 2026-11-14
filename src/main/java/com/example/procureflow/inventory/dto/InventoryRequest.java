package com.example.procureflow.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record InventoryRequest(
        @NotNull(message = "Item ID is required")
        Long itemId,

        @NotNull(message = "Quantity is required")
        @Min(value = 0, message = "Quantity must be >= 0")
        Integer quantity,

        String location
) {
}
