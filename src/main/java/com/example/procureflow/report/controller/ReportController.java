package com.example.procureflow.report.controller;

import com.example.procureflow.report.dto.*;
import com.example.procureflow.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "Reports & Dashboard", description = "Procurement, expenditure, and inventory reporting endpoints")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/dashboard/summary")
    @Operation(summary = "Get high-level dashboard summary metrics (scoped by role)")
    public ResponseEntity<DashboardSummaryResponse> getSummary() {
        return ResponseEntity.ok(reportService.getDashboardSummary());
    }

    @GetMapping("/reports/expenditure")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE_OFFICER', 'PROCUREMENT_OFFICER', 'DEPARTMENT_MANAGER')")
    @Operation(summary = "Get expenditure report grouped by department and supplier")
    public ResponseEntity<ExpenditureReportResponse> getExpenditureReport(@ModelAttribute ReportFilter filter) {
        return ResponseEntity.ok(reportService.getExpenditureReport(filter));
    }

    @GetMapping("/reports/requisitions")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE_OFFICER', 'PROCUREMENT_OFFICER', 'DEPARTMENT_MANAGER')")
    @Operation(summary = "Get requisitions report by status, priority, and department")
    public ResponseEntity<RequisitionReportResponse> getRequisitionReport(@ModelAttribute ReportFilter filter) {
        return ResponseEntity.ok(reportService.getRequisitionReport(filter));
    }

    @GetMapping("/reports/inventory-valuation")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE_OFFICER', 'PROCUREMENT_OFFICER', 'STORE_KEEPER')")
    @Operation(summary = "Get inventory valuation report")
    public ResponseEntity<InventoryValuationReportResponse> getInventoryValuationReport(@ModelAttribute ReportFilter filter) {
        return ResponseEntity.ok(reportService.getInventoryValuationReport(filter));
    }

    @GetMapping("/reports/suppliers")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE_OFFICER', 'PROCUREMENT_OFFICER')")
    @Operation(summary = "Get supplier procurement statistics report")
    public ResponseEntity<SupplierReportResponse> getSupplierReport(@ModelAttribute ReportFilter filter) {
        return ResponseEntity.ok(reportService.getSupplierReport(filter));
    }

    @GetMapping("/reports/departments")
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE_OFFICER', 'PROCUREMENT_OFFICER', 'DEPARTMENT_MANAGER')")
    @Operation(summary = "Get department procurement and budget utilization report")
    public ResponseEntity<DepartmentReportResponse> getDepartmentReport(@ModelAttribute ReportFilter filter) {
        return ResponseEntity.ok(reportService.getDepartmentReport(filter));
    }
}
