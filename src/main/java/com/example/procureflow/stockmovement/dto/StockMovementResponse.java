package com.example.procureflow.stockmovement.dto;

import com.example.procureflow.stockmovement.entity.StockMovement;

import java.time.Instant;

public record StockMovementResponse(
        Long id,
        Long itemId,
        String itemSku,
        String itemName,
        String movementType,
        int quantity,
        String referenceType,
        Long referenceId,
        Long departmentId,
        String departmentName,
        Long recipientId,
        String recipientName,
        Long performedById,
        String performedByName,
        String note,
        Instant createdAt
) {
    public static StockMovementResponse fromEntity(StockMovement m) {
        return new StockMovementResponse(
                m.getId(),
                m.getItem() != null ? m.getItem().getId() : null,
                m.getItem() != null ? m.getItem().getSku() : null,
                m.getItem() != null ? m.getItem().getName() : null,
                m.getMovementType() != null ? m.getMovementType().name() : null,
                m.getQuantity(),
                m.getReferenceType(),
                m.getReferenceId(),
                m.getDepartment() != null ? m.getDepartment().getId() : null,
                m.getDepartment() != null ? m.getDepartment().getName() : null,
                m.getRecipient() != null ? m.getRecipient().getId() : null,
                m.getRecipient() != null ? m.getRecipient().getFullName() : null,
                m.getPerformedBy() != null ? m.getPerformedBy().getId() : null,
                m.getPerformedBy() != null ? m.getPerformedBy().getFullName() : null,
                m.getNote(),
                m.getCreatedAt()
        );
    }
}
