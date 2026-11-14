package com.example.procureflow.purchaseorder.service;

import com.example.procureflow.common.dto.PageResponse;
import com.example.procureflow.common.exception.BusinessException;
import com.example.procureflow.common.exception.ResourceNotFoundException;
import com.example.procureflow.item.entity.Item;
import com.example.procureflow.item.repository.ItemRepository;
import com.example.procureflow.purchaseorder.dto.PurchaseOrderRequest;
import com.example.procureflow.purchaseorder.dto.PurchaseOrderResponse;
import com.example.procureflow.purchaseorder.entity.PurchaseOrder;
import com.example.procureflow.purchaseorder.entity.PurchaseOrderItem;
import com.example.procureflow.purchaseorder.entity.PurchaseOrderStatus;
import com.example.procureflow.purchaseorder.repository.PurchaseOrderRepository;
import com.example.procureflow.quotation.entity.Quotation;
import com.example.procureflow.quotation.repository.QuotationRepository;
import com.example.procureflow.requisition.entity.Requisition;
import com.example.procureflow.requisition.entity.RequisitionItem;
import com.example.procureflow.requisition.entity.RequisitionStatus;
import com.example.procureflow.requisition.repository.RequisitionRepository;
import com.example.procureflow.requisition.service.RequisitionWorkflowService;
import com.example.procureflow.security.CurrentUser;
import com.example.procureflow.supplier.entity.Supplier;
import com.example.procureflow.supplier.repository.SupplierRepository;
import com.example.procureflow.user.entity.AppUser;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;

@Service
public class PurchaseOrderService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final RequisitionRepository requisitionRepository;
    private final QuotationRepository quotationRepository;
    private final SupplierRepository supplierRepository;
    private final ItemRepository itemRepository;
    private final RequisitionWorkflowService workflowService;
    private final CurrentUser currentUser;

    public PurchaseOrderService(
            PurchaseOrderRepository purchaseOrderRepository,
            RequisitionRepository requisitionRepository,
            QuotationRepository quotationRepository,
            SupplierRepository supplierRepository,
            ItemRepository itemRepository,
            RequisitionWorkflowService workflowService,
            CurrentUser currentUser
    ) {
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.requisitionRepository = requisitionRepository;
        this.quotationRepository = quotationRepository;
        this.supplierRepository = supplierRepository;
        this.itemRepository = itemRepository;
        this.workflowService = workflowService;
        this.currentUser = currentUser;
    }

    @Transactional
    public PurchaseOrderResponse create(PurchaseOrderRequest request) {
        Requisition req = requisitionRepository.findById(request.requisitionId())
                .orElseThrow(() -> new ResourceNotFoundException("Requisition not found with id: " + request.requisitionId()));

        if (req.getStatus() != RequisitionStatus.PROCUREMENT) {
            throw new BusinessException(
                    "Cannot issue PO for requisition in status: " + req.getStatus() +
                    ". Requisition must be in PROCUREMENT stage."
            );
        }

        // Selected quotation check
        Quotation quotation = null;
        if (request.quotationId() != null) {
            quotation = quotationRepository.findById(request.quotationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Quotation not found with id: " + request.quotationId()));
        } else {
            quotation = quotationRepository.findByRequisitionIdAndSelectedTrue(req.getId())
                    .orElseThrow(() -> new BusinessException("A selected supplier quotation is required before issuing a purchase order"));
        }

        Supplier supplier = quotation.getSupplier();
        if (request.supplierId() != null) {
            supplier = supplierRepository.findById(request.supplierId())
                    .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + request.supplierId()));
        }

        AppUser actor = currentUser.getUser();

        long seq = purchaseOrderRepository.getNextSequenceValue();
        String poNumber = String.format("PO-%d-%06d", Year.now().getValue(), seq);

        PurchaseOrder po = new PurchaseOrder();
        po.setPoNumber(poNumber);
        po.setRequisition(req);
        po.setSupplier(supplier);
        po.setQuotation(quotation);
        po.setStatus(PurchaseOrderStatus.ORDERED);
        po.setExpectedDeliveryDate(request.expectedDeliveryDate());
        po.setCreatedBy(actor);

        BigDecimal total = BigDecimal.ZERO;

        if (request.items() != null && !request.items().isEmpty()) {
            for (PurchaseOrderRequest.PurchaseOrderItemRequest itemReq : request.items()) {
                Item item = itemRepository.findById(itemReq.itemId())
                        .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + itemReq.itemId()));
                BigDecimal unitPrice = itemReq.unitPrice() != null ? itemReq.unitPrice() : item.getUnitPrice();
                PurchaseOrderItem poItem = new PurchaseOrderItem(item, itemReq.quantity(), unitPrice);
                po.addItem(poItem);
                total = total.add(unitPrice.multiply(BigDecimal.valueOf(itemReq.quantity())));
            }
        } else {
            // Default to requisition items
            for (RequisitionItem reqItem : req.getItems()) {
                PurchaseOrderItem poItem = new PurchaseOrderItem(
                        reqItem.getItem(),
                        reqItem.getQuantity(),
                        reqItem.getEstimatedUnitPrice()
                );
                po.addItem(poItem);
                total = total.add(reqItem.getEstimatedUnitPrice().multiply(BigDecimal.valueOf(reqItem.getQuantity())));
            }
        }

        // If quotation has an explicit total amount, reflect it if requested or calculated
        if (total.compareTo(BigDecimal.ZERO) == 0 && quotation.getTotalAmount() != null) {
            total = quotation.getTotalAmount();
        }
        po.setTotalAmount(total);

        // Transition requisition to ORDERED
        workflowService.issuePo(req);

        PurchaseOrder saved = purchaseOrderRepository.save(po);
        return PurchaseOrderResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<PurchaseOrderResponse> findAll(String search, PurchaseOrderStatus status, Long supplierId, Pageable pageable) {
        Specification<PurchaseOrder> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (search != null && !search.trim().isEmpty()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("poNumber")), pattern),
                        cb.like(cb.lower(root.get("supplier").get("name")), pattern)
                ));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (supplierId != null) {
                predicates.add(cb.equal(root.get("supplier").get("id"), supplierId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<PurchaseOrderResponse> page = purchaseOrderRepository.findAll(spec, pageable)
                .map(PurchaseOrderResponse::fromEntity);
        return PageResponse.of(page);
    }

    @Transactional(readOnly = true)
    public PurchaseOrderResponse findById(Long id) {
        PurchaseOrder po = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase order not found with id: " + id));
        return PurchaseOrderResponse.fromEntity(po);
    }
}
