package com.example.procureflow.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record StockIssueRequest(
        @NotNull(message = "Item ID is required")
        Long itemId,

        @NotNull(message = "Quantity is required")
        @Min(value = 1, message = "Quantity must be greater than 0")
        Integer quantity,

        @NotNull(message = "Department ID is required")
        Long departmentId,

        @NotBlank(message = "Recipient name is required")
        String recipientName,

        @NotBlank(message = "Reason is required")
        String reason
) {
}
