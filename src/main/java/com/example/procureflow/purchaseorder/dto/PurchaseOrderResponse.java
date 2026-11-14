package com.example.procureflow.purchaseorder.dto;

import com.example.procureflow.purchaseorder.entity.PurchaseOrder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record PurchaseOrderResponse(
        Long id,
        String poNumber,
        Long requisitionId,
        String requisitionNumber,
        Long supplierId,
        String supplierName,
        Long quotationId,
        String status,
        LocalDate expectedDeliveryDate,
        BigDecimal totalAmount,
        Long createdById,
        String createdByName,
        List<PurchaseOrderItemResponse> items,
        Instant createdAt,
        Instant updatedAt
) {
    public record PurchaseOrderItemResponse(
            Long id,
            Long itemId,
            String itemSku,
            String itemName,
            String itemUnit,
            int quantity,
            int receivedQuantity,
            BigDecimal unitPrice,
            BigDecimal subtotal
    ) {}

    public static PurchaseOrderResponse fromEntity(PurchaseOrder po) {
        List<PurchaseOrderItemResponse> itemResponses = po.getItems() != null
                ? po.getItems().stream()
                .map(i -> new PurchaseOrderItemResponse(
                        i.getId(),
                        i.getItem() != null ? i.getItem().getId() : null,
                        i.getItem() != null ? i.getItem().getSku() : null,
                        i.getItem() != null ? i.getItem().getName() : null,
                        i.getItem() != null ? i.getItem().getUnit() : null,
                        i.getQuantity(),
                        i.getReceivedQuantity(),
                        i.getUnitPrice(),
                        i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity()))
                ))
                .toList()
                : List.of();

        return new PurchaseOrderResponse(
                po.getId(),
                po.getPoNumber(),
                po.getRequisition() != null ? po.getRequisition().getId() : null,
                po.getRequisition() != null ? po.getRequisition().getRequisitionNumber() : null,
                po.getSupplier() != null ? po.getSupplier().getId() : null,
                po.getSupplier() != null ? po.getSupplier().getName() : null,
                po.getQuotation() != null ? po.getQuotation().getId() : null,
                po.getStatus() != null ? po.getStatus().name() : null,
                po.getExpectedDeliveryDate(),
                po.getTotalAmount(),
                po.getCreatedBy() != null ? po.getCreatedBy().getId() : null,
                po.getCreatedBy() != null ? po.getCreatedBy().getFullName() : null,
                itemResponses,
                po.getCreatedAt(),
                po.getUpdatedAt()
        );
    }
}
