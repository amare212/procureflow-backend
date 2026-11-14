package com.example.procureflow.supplier.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record SupplierRequest(
        @NotBlank(message = "Supplier name is required")
        @Size(max = 150, message = "Name must not exceed 150 characters")
        String name,

        @Size(max = 150, message = "Contact name must not exceed 150 characters")
        String contactName,

        @Email(message = "Invalid email format")
        @Size(max = 180, message = "Email must not exceed 180 characters")
        String email,

        @Size(max = 40, message = "Phone must not exceed 40 characters")
        String phone,

        @Size(max = 255, message = "Address must not exceed 255 characters")
        String address,

        String status,

        String notes,

        Set<Long> categoryIds
) {
}
