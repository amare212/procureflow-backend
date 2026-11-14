package com.example.procureflow.supplier.controller;

import com.example.procureflow.common.dto.PageResponse;
import com.example.procureflow.supplier.dto.SupplierPurchaseHistoryResponse;
import com.example.procureflow.supplier.dto.SupplierRequest;
import com.example.procureflow.supplier.dto.SupplierResponse;
import com.example.procureflow.supplier.service.SupplierService;
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
@RequestMapping("/api/suppliers")
@Tag(name = "Suppliers", description = "Supplier management and history endpoints")
public class SupplierController {

    private final SupplierService supplierService;

    public SupplierController(SupplierService supplierService) {
        this.supplierService = supplierService;
    }

    @GetMapping
    @Operation(summary = "List suppliers with search, status filtering, and pagination")
    public ResponseEntity<PageResponse<SupplierResponse>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @PageableDefault(sort = "name", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return ResponseEntity.ok(supplierService.findAll(search, status, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get supplier by ID")
    public ResponseEntity<SupplierResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(supplierService.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_OFFICER')")
    @Operation(summary = "Create supplier (ADMIN or PROCUREMENT_OFFICER)")
    public ResponseEntity<SupplierResponse> create(@Valid @RequestBody SupplierRequest request) {
        SupplierResponse response = supplierService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_OFFICER')")
    @Operation(summary = "Update supplier (ADMIN or PROCUREMENT_OFFICER)")
    public ResponseEntity<SupplierResponse> update(@PathVariable Long id, @Valid @RequestBody SupplierRequest request) {
        return ResponseEntity.ok(supplierService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete supplier (ADMIN only)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        supplierService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/purchase-history")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_OFFICER', 'FINANCE_OFFICER')")
    @Operation(summary = "Get supplier purchase history and spend statistics")
    public ResponseEntity<SupplierPurchaseHistoryResponse> getPurchaseHistory(@PathVariable Long id) {
        return ResponseEntity.ok(supplierService.getPurchaseHistory(id));
    }
}
