package com.example.procureflow.requisition.controller;

import com.example.procureflow.approval.dto.ApprovalRequest;
import com.example.procureflow.approval.dto.ApprovalResponse;
import com.example.procureflow.common.dto.PageResponse;
import com.example.procureflow.requisition.dto.RequisitionRequest;
import com.example.procureflow.requisition.dto.RequisitionResponse;
import com.example.procureflow.requisition.entity.RequisitionStatus;
import com.example.procureflow.requisition.service.RequisitionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/requisitions")
@Tag(name = "Requisitions", description = "Purchase requisition and approval lifecycle endpoints")
public class RequisitionController {

    private final RequisitionService requisitionService;

    public RequisitionController(RequisitionService requisitionService) {
        this.requisitionService = requisitionService;
    }

    @PostMapping
    @Operation(summary = "Create purchase requisition (starts in DRAFT)")
    public ResponseEntity<RequisitionResponse> create(@Valid @RequestBody RequisitionRequest request) {
        RequisitionResponse response = requisitionService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "List purchase requisitions with filtering and pagination")
    public ResponseEntity<PageResponse<RequisitionResponse>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) RequisitionStatus status,
            @RequestParam(required = false) Long departmentId,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(requisitionService.findAll(search, status, departmentId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get purchase requisition details by ID")
    public ResponseEntity<RequisitionResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(requisitionService.findById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update purchase requisition (requester only, DRAFT only)")
    public ResponseEntity<RequisitionResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody RequisitionRequest request
    ) {
        return ResponseEntity.ok(requisitionService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete purchase requisition (requester only, DRAFT only)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        requisitionService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/submit")
    @Operation(summary = "Submit purchase requisition for manager review")
    public ResponseEntity<RequisitionResponse> submit(@PathVariable Long id) {
        return ResponseEntity.ok(requisitionService.submit(id));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve requisition (Manager or Finance stage)")
    public ResponseEntity<RequisitionResponse> approve(
            @PathVariable Long id,
            @RequestBody(required = false) ApprovalRequest request
    ) {
        String comment = request != null ? request.comment() : null;
        return ResponseEntity.ok(requisitionService.approve(id, comment));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject requisition (comment required)")
    public ResponseEntity<RequisitionResponse> reject(
            @PathVariable Long id,
            @Valid @RequestBody ApprovalRequest request
    ) {
        String comment = request != null ? request.comment() : null;
        return ResponseEntity.ok(requisitionService.reject(id, comment));
    }

    @GetMapping("/{id}/approvals")
    @Operation(summary = "Get approval audit trail for requisition")
    public ResponseEntity<List<ApprovalResponse>> getApprovals(@PathVariable Long id) {
        return ResponseEntity.ok(requisitionService.getApprovals(id));
    }
}
