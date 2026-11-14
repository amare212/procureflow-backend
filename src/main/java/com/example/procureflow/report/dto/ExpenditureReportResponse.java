package com.example.procureflow.report.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ExpenditureReportResponse(
        BigDecimal totalSpend,
        LocalDate periodFrom,
        LocalDate periodTo,
        List<DepartmentSpend> byDepartment,
        List<SupplierSpend> bySupplier
) {
    public record DepartmentSpend(
            Long departmentId,
            String departmentName,
            BigDecimal spend,
            int orderCount
    ) {}

    public record SupplierSpend(
            Long supplierId,
            String supplierName,
            BigDecimal spend,
            int orderCount
    ) {}
}
