package com.example.procureflow.inventory.dto;

import com.example.procureflow.inventory.entity.Inventory;

import java.math.BigDecimal;
import java.time.Instant;

public record InventoryResponse(
        Long id,
        Long itemId,
        String itemSku,
        String itemName,
        String unit,
        BigDecimal unitPrice,
        int quantity,
        int reorderLevel,
        boolean lowStock,
        String location,
        Instant updatedAt
) {
    public static InventoryResponse fromEntity(Inventory inv) {
        boolean isLow = inv.getItem() != null && inv.getQuantity() <= inv.getItem().getReorderLevel();
        return new InventoryResponse(
                inv.getId(),
                inv.getItem() != null ? inv.getItem().getId() : null,
                inv.getItem() != null ? inv.getItem().getSku() : null,
                inv.getItem() != null ? inv.getItem().getName() : null,
                inv.getItem() != null ? inv.getItem().getUnit() : null,
                inv.getItem() != null ? inv.getItem().getUnitPrice() : null,
                inv.getQuantity(),
                inv.getItem() != null ? inv.getItem().getReorderLevel() : 0,
                isLow,
                inv.getLocation(),
                inv.getUpdatedAt()
        );
    }
}
