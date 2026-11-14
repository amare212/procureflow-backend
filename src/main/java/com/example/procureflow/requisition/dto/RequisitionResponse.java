package com.example.procureflow.requisition.dto;

import com.example.procureflow.requisition.entity.Requisition;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record RequisitionResponse(
        Long id,
        String requisitionNumber,
        Long employeeId,
        String employeeName,
        Long departmentId,
        String departmentName,
        String justification,
        String priority,
        LocalDate requiredDate,
        String status,
        Long version,
        BigDecimal estimatedTotal,
        List<RequisitionItemResponse> items,
        Instant createdAt,
        Instant updatedAt
) {
    public record RequisitionItemResponse(
            Long id,
            Long itemId,
            String itemSku,
            String itemName,
            String itemUnit,
            int quantity,
            BigDecimal estimatedUnitPrice,
            BigDecimal subtotal
    ) {}

    public static RequisitionResponse fromEntity(Requisition req) {
        List<RequisitionItemResponse> itemResponses = req.getItems() != null
                ? req.getItems().stream()
                .map(i -> new RequisitionItemResponse(
                        i.getId(),
                        i.getItem() != null ? i.getItem().getId() : null,
                        i.getItem() != null ? i.getItem().getSku() : null,
                        i.getItem() != null ? i.getItem().getName() : null,
                        i.getItem() != null ? i.getItem().getUnit() : null,
                        i.getQuantity(),
                        i.getEstimatedUnitPrice(),
                        i.getEstimatedUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity()))
                ))
                .toList()
                : List.of();

        return new RequisitionResponse(
                req.getId(),
                req.getRequisitionNumber(),
                req.getEmployee() != null ? req.getEmployee().getId() : null,
                req.getEmployee() != null ? req.getEmployee().getFullName() : null,
                req.getDepartment() != null ? req.getDepartment().getId() : null,
                req.getDepartment() != null ? req.getDepartment().getName() : null,
                req.getJustification(),
                req.getPriority(),
                req.getRequiredDate(),
                req.getStatus() != null ? req.getStatus().name() : null,
                req.getVersion(),
                req.getEstimatedTotal(),
                itemResponses,
                req.getCreatedAt(),
                req.getUpdatedAt()
        );
    }
}
