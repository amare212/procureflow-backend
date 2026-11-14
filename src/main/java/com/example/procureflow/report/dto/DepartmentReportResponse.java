package com.example.procureflow.report.dto;

import java.math.BigDecimal;
import java.util.List;

public record DepartmentReportResponse(
        int totalDepartments,
        List<DepartmentStatDetail> departments
) {
    public record DepartmentStatDetail(
            Long departmentId,
            String departmentName,
            String code,
            BigDecimal budget,
            BigDecimal totalSpend,
            BigDecimal remainingBudget,
            long requisitionsCount
    ) {}
}
