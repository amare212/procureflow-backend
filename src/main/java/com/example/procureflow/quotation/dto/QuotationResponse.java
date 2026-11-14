package com.example.procureflow.quotation.dto;

import com.example.procureflow.quotation.entity.Quotation;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record QuotationResponse(
        Long id,
        Long requisitionId,
        Long supplierId,
        String supplierName,
        BigDecimal totalAmount,
        LocalDate validUntil,
        boolean selected,
        String notes,
        Long createdByUserId,
        String createdByName,
        Instant createdAt
) {
    public static QuotationResponse fromEntity(Quotation q) {
        return new QuotationResponse(
                q.getId(),
                q.getRequisition() != null ? q.getRequisition().getId() : null,
                q.getSupplier() != null ? q.getSupplier().getId() : null,
                q.getSupplier() != null ? q.getSupplier().getName() : null,
                q.getTotalAmount(),
                q.getValidUntil(),
                q.isSelected(),
                q.getNotes(),
                q.getCreatedBy() != null ? q.getCreatedBy().getId() : null,
                q.getCreatedBy() != null ? q.getCreatedBy().getFullName() : null,
                q.getCreatedAt()
        );
    }
}
