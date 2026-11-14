package com.example.procureflow.quotation.controller;

import com.example.procureflow.quotation.dto.QuotationRequest;
import com.example.procureflow.quotation.dto.QuotationResponse;
import com.example.procureflow.quotation.service.QuotationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Quotations", description = "Supplier quotation management")
public class QuotationController {

    private final QuotationService quotationService;

    public QuotationController(QuotationService quotationService) {
        this.quotationService = quotationService;
    }

    @PostMapping("/requisitions/{id}/quotations")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_OFFICER')")
    @Operation(summary = "Add supplier quotation for a requisition in PROCUREMENT stage")
    public ResponseEntity<QuotationResponse> createQuotation(
            @PathVariable Long id,
            @Valid @RequestBody QuotationRequest request
    ) {
        QuotationResponse response = quotationService.createQuotation(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/requisitions/{id}/quotations")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_OFFICER', 'FINANCE_OFFICER')")
    @Operation(summary = "List supplier quotations for a requisition")
    public ResponseEntity<List<QuotationResponse>> listQuotations(@PathVariable Long id) {
        return ResponseEntity.ok(quotationService.findByRequisitionId(id));
    }

    @PostMapping("/quotations/{id}/select")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROCUREMENT_OFFICER')")
    @Operation(summary = "Select winning quotation for a requisition")
    public ResponseEntity<QuotationResponse> selectQuotation(@PathVariable Long id) {
        return ResponseEntity.ok(quotationService.selectQuotation(id));
    }
}
