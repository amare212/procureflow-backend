package com.example.procureflow.goodsreceipt.dto;

import com.example.procureflow.goodsreceipt.entity.GoodsReceipt;

import java.time.Instant;
import java.util.List;

public record GoodsReceiptResponse(
        Long id,
        String receiptNumber,
        Long purchaseOrderId,
        String poNumber,
        String idempotencyKey,
        String deliveryNoteNumber,
        Long receivedById,
        String receivedByName,
        Instant receivedAt,
        boolean verified,
        String notes,
        List<GoodsReceiptItemResponse> items
) {
    public record GoodsReceiptItemResponse(
            Long id,
            Long purchaseOrderItemId,
            Long itemId,
            String itemSku,
            String itemName,
            String itemUnit,
            int quantityReceived
    ) {}

    public static GoodsReceiptResponse fromEntity(GoodsReceipt gr) {
        List<GoodsReceiptItemResponse> itemResponses = gr.getItems() != null
                ? gr.getItems().stream()
                .map(i -> new GoodsReceiptItemResponse(
                        i.getId(),
                        i.getPurchaseOrderItem() != null ? i.getPurchaseOrderItem().getId() : null,
                        i.getItem() != null ? i.getItem().getId() : null,
                        i.getItem() != null ? i.getItem().getSku() : null,
                        i.getItem() != null ? i.getItem().getName() : null,
                        i.getItem() != null ? i.getItem().getUnit() : null,
                        i.getQuantityReceived()
                ))
                .toList()
                : List.of();

        return new GoodsReceiptResponse(
                gr.getId(),
                gr.getReceiptNumber(),
                gr.getPurchaseOrder() != null ? gr.getPurchaseOrder().getId() : null,
                gr.getPurchaseOrder() != null ? gr.getPurchaseOrder().getPoNumber() : null,
                gr.getIdempotencyKey(),
                gr.getDeliveryNoteNumber(),
                gr.getReceivedBy() != null ? gr.getReceivedBy().getId() : null,
                gr.getReceivedBy() != null ? gr.getReceivedBy().getFullName() : null,
                gr.getReceivedAt(),
                gr.isVerified(),
                gr.getNotes(),
                itemResponses
        );
    }
}
