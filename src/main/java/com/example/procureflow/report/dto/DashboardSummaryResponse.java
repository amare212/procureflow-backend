package com.example.procureflow.report.dto;

import java.math.BigDecimal;

public record DashboardSummaryResponse(
        long totalRequisitions,
        long pendingApprovals,
        long totalPurchaseOrders,
        long outstandingOrders,
        long totalInventoryItems,
        long lowStockItemsCount,
        BigDecimal totalSpend
) {
}
