package com.example.procureflow.approval.controller;

import com.example.procureflow.approval.dto.ApprovalResponse;
import com.example.procureflow.approval.service.ApprovalService;
import com.example.procureflow.common.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/approvals")
@Tag(name = "Approvals", description = "Requisition approvals audit log")
public class ApprovalController {

    private final ApprovalService approvalService;

    public ApprovalController(ApprovalService approvalService) {
        this.approvalService = approvalService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'FINANCE_OFFICER', 'DEPARTMENT_MANAGER')")
    @Operation(summary = "List approvals with pagination")
    public ResponseEntity<PageResponse<ApprovalResponse>> list(
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(approvalService.findAll(pageable));
    }
}
