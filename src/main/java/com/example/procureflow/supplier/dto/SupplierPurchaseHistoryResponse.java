package com.example.procureflow.supplier.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record SupplierPurchaseHistoryResponse(
        Long supplierId,
        String supplierName,
        int totalOrders,
        BigDecimal totalSpent,
        List<PurchaseOrderSummary> orders
) {
    public record PurchaseOrderSummary(
            Long id,
            String poNumber,
            String status,
            BigDecimal totalAmount,
            Instant createdAt
    ) {}
}
