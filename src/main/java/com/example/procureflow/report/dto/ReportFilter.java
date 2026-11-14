package com.example.procureflow.report.dto;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record ReportFilter(
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate from,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate to,

        Long departmentId,
        Long supplierId,
        String status,
        Long categoryId
) {
}
