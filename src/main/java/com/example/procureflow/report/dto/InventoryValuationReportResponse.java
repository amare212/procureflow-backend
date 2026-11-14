package com.example.procureflow.report.dto;

import java.math.BigDecimal;
import java.util.List;

public record InventoryValuationReportResponse(
        long totalItems,
        long totalQuantity,
        BigDecimal totalValuation,
        List<ItemValuationDetail> items
) {
    public record ItemValuationDetail(
            Long itemId,
            String sku,
            String name,
            String categoryName,
            int quantity,
            BigDecimal unitPrice,
            BigDecimal totalValue,
            boolean lowStock
    ) {}
}
