package com.example.procureflow.quotation.service;

import com.example.procureflow.common.exception.BusinessException;
import com.example.procureflow.common.exception.ResourceNotFoundException;
import com.example.procureflow.quotation.dto.QuotationRequest;
import com.example.procureflow.quotation.dto.QuotationResponse;
import com.example.procureflow.quotation.entity.Quotation;
import com.example.procureflow.quotation.repository.QuotationRepository;
import com.example.procureflow.requisition.entity.Requisition;
import com.example.procureflow.requisition.entity.RequisitionStatus;
import com.example.procureflow.requisition.repository.RequisitionRepository;
import com.example.procureflow.security.CurrentUser;
import com.example.procureflow.supplier.entity.Supplier;
import com.example.procureflow.supplier.repository.SupplierRepository;
import com.example.procureflow.user.entity.AppUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class QuotationService {

    private final QuotationRepository quotationRepository;
    private final RequisitionRepository requisitionRepository;
    private final SupplierRepository supplierRepository;
    private final CurrentUser currentUser;

    public QuotationService(
            QuotationRepository quotationRepository,
            RequisitionRepository requisitionRepository,
            SupplierRepository supplierRepository,
            CurrentUser currentUser
    ) {
        this.quotationRepository = quotationRepository;
        this.requisitionRepository = requisitionRepository;
        this.supplierRepository = supplierRepository;
        this.currentUser = currentUser;
    }

    @Transactional
    public QuotationResponse createQuotation(Long requisitionId, QuotationRequest request) {
        Requisition req = requisitionRepository.findById(requisitionId)
                .orElseThrow(() -> new ResourceNotFoundException("Requisition not found with id: " + requisitionId));

        if (req.getStatus() != RequisitionStatus.PROCUREMENT) {
            throw new BusinessException(
                    "Cannot add quotations for requisition in status: " + req.getStatus() +
                    ". Requisition must be in PROCUREMENT stage."
            );
        }

        Supplier supplier = supplierRepository.findById(request.supplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + request.supplierId()));

        AppUser actor = currentUser.getUser();

        Quotation quotation = new Quotation();
        quotation.setRequisition(req);
        quotation.setSupplier(supplier);
        quotation.setTotalAmount(request.totalAmount());
        quotation.setValidUntil(request.validUntil());
        quotation.setNotes(request.notes());
        quotation.setCreatedBy(actor);
        quotation.setSelected(false);

        return QuotationResponse.fromEntity(quotationRepository.save(quotation));
    }

    @Transactional(readOnly = true)
    public List<QuotationResponse> findByRequisitionId(Long requisitionId) {
        if (!requisitionRepository.existsById(requisitionId)) {
            throw new ResourceNotFoundException("Requisition not found with id: " + requisitionId);
        }
        return quotationRepository.findByRequisitionIdOrderByCreatedAtDesc(requisitionId)
                .stream()
                .map(QuotationResponse::fromEntity)
                .toList();
    }

    @Transactional
    public QuotationResponse selectQuotation(Long quotationId) {
        Quotation target = quotationRepository.findById(quotationId)
                .orElseThrow(() -> new ResourceNotFoundException("Quotation not found with id: " + quotationId));

        Requisition req = target.getRequisition();
        if (req.getStatus() != RequisitionStatus.PROCUREMENT) {
            throw new BusinessException("Cannot select quotation for requisition in status: " + req.getStatus());
        }

        // Deselect other quotations for this requisition
        List<Quotation> existing = quotationRepository.findByRequisitionIdOrderByCreatedAtDesc(req.getId());
        for (Quotation q : existing) {
            q.setSelected(q.getId().equals(quotationId));
            quotationRepository.save(q);
        }

        target.setSelected(true);
        return QuotationResponse.fromEntity(quotationRepository.save(target));
    }
}
