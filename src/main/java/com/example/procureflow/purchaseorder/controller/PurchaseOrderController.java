package com.example.procureflow.purchaseorder.controller;

import com.example.procureflow.common.dto.PageResponse;
import com.example.procureflow.purchaseorder.dto.PurchaseOrderRequest;
import com.example.procureflow.purchaseorder.dto.PurchaseOrderResponse;
import com.example.procureflow.purchaseorder.entity.PurchaseOrderStatus;
import com.example.procureflow.purchaseorder.service.PurchaseOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/purchase-orders")
@Tag(name = "Purchase Orders", description = "Purchase order issuance and tracking endpoints")
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    public PurchaseOrderController(PurchaseOrderService purchaseOrderService) {
        this.purchaseOrderService = purchaseOrderService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_OFFICER')")
    @Operation(summary = "Generate purchase order from approved requisition with selected quotation")
    public ResponseEntity<PurchaseOrderResponse> create(@Valid @RequestBody PurchaseOrderRequest request) {
        PurchaseOrderResponse response = purchaseOrderService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_OFFICER', 'FINANCE_OFFICER', 'STORE_KEEPER', 'DEPARTMENT_MANAGER')")
    @Operation(summary = "List purchase orders with status/supplier filters and pagination")
    public ResponseEntity<PageResponse<PurchaseOrderResponse>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) PurchaseOrderStatus status,
            @RequestParam(required = false) Long supplierId,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(purchaseOrderService.findAll(search, status, supplierId, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_OFFICER', 'FINANCE_OFFICER', 'STORE_KEEPER', 'DEPARTMENT_MANAGER')")
    @Operation(summary = "Get purchase order details by ID")
    public ResponseEntity<PurchaseOrderResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(purchaseOrderService.findById(id));
    }
}
