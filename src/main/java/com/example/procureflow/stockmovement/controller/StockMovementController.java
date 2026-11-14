package com.example.procureflow.stockmovement.controller;

import com.example.procureflow.common.dto.PageResponse;
import com.example.procureflow.stockmovement.dto.StockMovementResponse;
import com.example.procureflow.stockmovement.entity.MovementType;
import com.example.procureflow.stockmovement.service.StockMovementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stock-movements")
@Tag(name = "Stock Movements", description = "Auditable stock movement logs (receipts, issues, adjustments)")
public class StockMovementController {

    private final StockMovementService stockMovementService;

    public StockMovementController(StockMovementService stockMovementService) {
        this.stockMovementService = stockMovementService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_KEEPER', 'PROCUREMENT_OFFICER', 'FINANCE_OFFICER')")
    @Operation(summary = "List audit stock movements with filtering and pagination")
    public ResponseEntity<PageResponse<StockMovementResponse>> list(
            @RequestParam(required = false) Long itemId,
            @RequestParam(required = false) MovementType movementType,
            @PageableDefault(sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(stockMovementService.findAll(itemId, movementType, pageable));
    }
}
