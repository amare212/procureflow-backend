package com.example.procureflow.purchaseorder.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record PurchaseOrderRequest(
        @NotNull(message = "Requisition ID is required")
        Long requisitionId,

        Long supplierId,

        Long quotationId,

        LocalDate expectedDeliveryDate,

        List<PurchaseOrderItemRequest> items
) {
    public record PurchaseOrderItemRequest(
            @NotNull Long itemId,
            @NotNull Integer quantity,
            java.math.BigDecimal unitPrice
    ) {}
}
