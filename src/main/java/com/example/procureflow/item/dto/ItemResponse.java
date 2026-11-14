package com.example.procureflow.item.dto;

import com.example.procureflow.item.entity.Item;

import java.math.BigDecimal;
import java.time.Instant;

public record ItemResponse(
        Long id,
        String sku,
        String name,
        String description,
        Long categoryId,
        String categoryName,
        String unit,
        BigDecimal unitPrice,
        int reorderLevel,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
    public static ItemResponse fromEntity(Item item) {
        return new ItemResponse(
                item.getId(),
                item.getSku(),
                item.getName(),
                item.getDescription(),
                item.getCategory() != null ? item.getCategory().getId() : null,
                item.getCategory() != null ? item.getCategory().getName() : null,
                item.getUnit(),
                item.getUnitPrice(),
                item.getReorderLevel(),
                item.isActive(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }
}
