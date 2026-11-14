package com.example.procureflow.supplier.dto;

import com.example.procureflow.category.entity.ItemCategory;
import com.example.procureflow.supplier.entity.Supplier;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

public record SupplierResponse(
        Long id,
        String name,
        String contactName,
        String email,
        String phone,
        String address,
        String status,
        String notes,
        Set<Long> categoryIds,
        Set<String> categoryNames,
        Instant createdAt,
        Instant updatedAt
) {
    public static SupplierResponse fromEntity(Supplier supplier) {
        Set<Long> catIds = supplier.getCategories() != null
                ? supplier.getCategories().stream().map(ItemCategory::getId).collect(Collectors.toSet())
                : Set.of();
        Set<String> catNames = supplier.getCategories() != null
                ? supplier.getCategories().stream().map(ItemCategory::getName).collect(Collectors.toSet())
                : Set.of();

        return new SupplierResponse(
                supplier.getId(),
                supplier.getName(),
                supplier.getContactName(),
                supplier.getEmail(),
                supplier.getPhone(),
                supplier.getAddress(),
                supplier.getStatus(),
                supplier.getNotes(),
                catIds,
                catNames,
                supplier.getCreatedAt(),
                supplier.getUpdatedAt()
        );
    }
}
