package com.example.procureflow.requisition.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record RequisitionRequest(
        @NotBlank(message = "Justification is required")
        String justification,

        String priority,

        LocalDate requiredDate,

        @NotEmpty(message = "At least one requisition item is required")
        @Valid
        List<RequisitionItemRequest> items
) {
    public record RequisitionItemRequest(
            @NotNull(message = "Item ID is required")
            Long itemId,

            @NotNull(message = "Quantity is required")
            @Min(value = 1, message = "Quantity must be greater than 0")
            Integer quantity,

            @NotNull(message = "Estimated unit price is required")
            @DecimalMin(value = "0.0", inclusive = true, message = "Estimated unit price must be >= 0")
            BigDecimal estimatedUnitPrice
    ) {}
}
