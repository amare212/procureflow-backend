package com.example.procureflow.report.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record RequisitionReportResponse(
        long totalCount,
        Map<String, Long> byStatus,
        Map<String, Long> byPriority,
        List<DepartmentRequisitionStat> byDepartment
) {
    public record DepartmentRequisitionStat(
            Long departmentId,
            String departmentName,
            long count,
            BigDecimal estimatedTotal
    ) {}
}
