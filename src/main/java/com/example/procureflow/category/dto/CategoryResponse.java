package com.example.procureflow.category.dto;

import com.example.procureflow.category.entity.ItemCategory;

public record CategoryResponse(
        Long id,
        String name,
        String description
) {
    public static CategoryResponse fromEntity(ItemCategory category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription()
        );
    }
}
