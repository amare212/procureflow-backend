package com.example.procureflow.goodsreceipt.controller;

import com.example.procureflow.common.dto.PageResponse;
import com.example.procureflow.goodsreceipt.dto.GoodsReceiptRequest;
import com.example.procureflow.goodsreceipt.dto.GoodsReceiptResponse;
import com.example.procureflow.goodsreceipt.service.GoodsReceiptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
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
@RequestMapping("/api")
@Tag(name = "Goods Receipts", description = "Transactional goods receiving and delivery verification")
public class GoodsReceiptController {

    private final GoodsReceiptService goodsReceiptService;

    public GoodsReceiptController(GoodsReceiptService goodsReceiptService) {
        this.goodsReceiptService = goodsReceiptService;
    }

    @PostMapping("/purchase-orders/{id}/receipts")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_KEEPER')")
    @Operation(
            summary = "Record goods receipt for purchase order (transactional, idempotency-protected)",
            description = "Receives goods against an open purchase order, updates inventory balances atomically, and creates auditable stock movement logs. Duplicate submissions with the same Idempotency-Key return the original receipt without duplicating stock."
    )
    public ResponseEntity<GoodsReceiptResponse> receive(
            @PathVariable Long id,
            @Parameter(in = ParameterIn.HEADER, name = "Idempotency-Key", description = "Unique UUID for duplicate submission protection")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody GoodsReceiptRequest request
    ) {
        GoodsReceiptResponse response = goodsReceiptService.receive(id, idempotencyKey, request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/goods-receipts")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_KEEPER', 'PROCUREMENT_OFFICER', 'FINANCE_OFFICER')")
    @Operation(summary = "List goods receipts with pagination")
    public ResponseEntity<PageResponse<GoodsReceiptResponse>> list(
            @PageableDefault(sort = "receivedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(goodsReceiptService.findAll(pageable));
    }

    @GetMapping("/goods-receipts/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STORE_KEEPER', 'PROCUREMENT_OFFICER', 'FINANCE_OFFICER')")
    @Operation(summary = "Get goods receipt by ID")
    public ResponseEntity<GoodsReceiptResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(goodsReceiptService.findById(id));
    }
}
