package com.example.procureflow.report.dto;

import java.math.BigDecimal;
import java.util.List;

public record SupplierReportResponse(
        int totalSuppliers,
        List<SupplierStatDetail> suppliers
) {
    public record SupplierStatDetail(
            Long supplierId,
            String supplierName,
            String status,
            int ordersCount,
            BigDecimal totalSpend
    ) {}
}
