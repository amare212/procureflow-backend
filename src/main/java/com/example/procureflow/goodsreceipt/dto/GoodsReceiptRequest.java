package com.example.procureflow.goodsreceipt.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record GoodsReceiptRequest(
        String deliveryNoteNumber,
        String notes,
        Boolean verified,

        @NotEmpty(message = "Receipt items must not be empty")
        @Valid
        List<ReceiptItemRequest> items
) {
    public record ReceiptItemRequest(
            @NotNull(message = "Purchase order item ID is required")
            Long purchaseOrderItemId,

            @NotNull(message = "Quantity received is required")
            @Min(value = 1, message = "Quantity received must be greater than 0")
            Integer quantityReceived
    ) {}
}
