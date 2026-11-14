package com.example.procureflow.quotation.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record QuotationRequest(
        @NotNull(message = "Supplier ID is required")
        Long supplierId,

        @NotNull(message = "Total amount is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Total amount must be >= 0")
        BigDecimal totalAmount,

        LocalDate validUntil,

        String notes
) {
}
