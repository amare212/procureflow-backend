package com.example.procureflow.inventory.controller;

import com.example.procureflow.common.dto.PageResponse;
import com.example.procureflow.inventory.dto.InventoryResponse;
import com.example.procureflow.inventory.dto.StockAdjustmentRequest;
import com.example.procureflow.inventory.dto.StockIssueRequest;
import com.example.procureflow.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory")
@Tag(name = "Inventory", description = "Inventory balance, issuing, adjustments, and low-stock tracking")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping
    @Operation(summary = "List current inventory balances with search and pagination")
    public ResponseEntity<PageResponse<InventoryResponse>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean lowStock,
            @PageableDefault(sort = "id", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return ResponseEntity.ok(inventoryService.findAll(search, lowStock, pageable));
    }

    @GetMapping("/low-stock")
    @Operation(summary = "List low-stock inventory items (quantity <= reorderLevel)")
    public ResponseEntity<PageResponse<InventoryResponse>> listLowStock(
            @PageableDefault(sort = "quantity", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return ResponseEntity.ok(inventoryService.findLowStock(pageable));
    }

    @GetMapping("/items/{itemId}")
    @Operation(summary = "Get inventory record for specific item")
    public ResponseEntity<InventoryResponse> getByItemId(@PathVariable Long itemId) {
        return ResponseEntity.ok(inventoryService.findByItemId(itemId));
    }

    @PostMapping("/issues")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_KEEPER')")
    @Operation(summary = "Issue inventory to a department (STORE_KEEPER only)")
    public ResponseEntity<InventoryResponse> issueStock(@Valid @RequestBody StockIssueRequest request) {
        return ResponseEntity.ok(inventoryService.issueStock(request));
    }

    @PostMapping("/adjustments")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_KEEPER')")
    @Operation(summary = "Adjust inventory count with audit reason (STORE_KEEPER or ADMIN)")
    public ResponseEntity<InventoryResponse> adjustStock(@Valid @RequestBody StockAdjustmentRequest request) {
        return ResponseEntity.ok(inventoryService.adjustStock(request));
    }
}
