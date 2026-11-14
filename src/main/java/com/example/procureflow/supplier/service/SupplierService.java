package com.example.procureflow.supplier.service;

import com.example.procureflow.category.entity.ItemCategory;
import com.example.procureflow.category.repository.CategoryRepository;
import com.example.procureflow.common.dto.PageResponse;
import com.example.procureflow.common.exception.BusinessException;
import com.example.procureflow.common.exception.ResourceNotFoundException;
import com.example.procureflow.purchaseorder.repository.PurchaseOrderRepository;
import com.example.procureflow.supplier.dto.SupplierPurchaseHistoryResponse;
import com.example.procureflow.supplier.dto.SupplierRequest;
import com.example.procureflow.supplier.dto.SupplierResponse;
import com.example.procureflow.supplier.entity.Supplier;
import com.example.procureflow.supplier.repository.SupplierRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final CategoryRepository categoryRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;

    public SupplierService(
            SupplierRepository supplierRepository,
            CategoryRepository categoryRepository,
            @Lazy PurchaseOrderRepository purchaseOrderRepository
    ) {
        this.supplierRepository = supplierRepository;
        this.categoryRepository = categoryRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<SupplierResponse> findAll(String search, String status, Pageable pageable) {
        Specification<Supplier> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (search != null && !search.trim().isEmpty()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("contactName")), pattern),
                        cb.like(cb.lower(root.get("email")), pattern)
                ));
            }
            if (status != null && !status.trim().isEmpty()) {
                predicates.add(cb.equal(cb.upper(root.get("status")), status.trim().toUpperCase()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<SupplierResponse> page = supplierRepository.findAll(spec, pageable)
                .map(SupplierResponse::fromEntity);
        return PageResponse.of(page);
    }

    @Transactional(readOnly = true)
    public SupplierResponse findById(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + id));
        return SupplierResponse.fromEntity(supplier);
    }

    @Transactional
    public SupplierResponse create(SupplierRequest request) {
        if (supplierRepository.existsByName(request.name().trim())) {
            throw new BusinessException("Supplier with name '" + request.name() + "' already exists");
        }

        Supplier supplier = new Supplier();
        supplier.setName(request.name().trim());
        supplier.setContactName(request.contactName());
        supplier.setEmail(request.email());
        supplier.setPhone(request.phone());
        supplier.setAddress(request.address());
        supplier.setStatus(request.status() != null ? request.status().toUpperCase() : "ACTIVE");
        supplier.setNotes(request.notes());

        if (request.categoryIds() != null && !request.categoryIds().isEmpty()) {
            Set<ItemCategory> categories = new HashSet<>(categoryRepository.findAllById(request.categoryIds()));
            supplier.setCategories(categories);
        }

        return SupplierResponse.fromEntity(supplierRepository.save(supplier));
    }

    @Transactional
    public SupplierResponse update(Long id, SupplierRequest request) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + id));

        if (!supplier.getName().equalsIgnoreCase(request.name().trim()) && supplierRepository.existsByName(request.name().trim())) {
            throw new BusinessException("Supplier with name '" + request.name() + "' already exists");
        }

        supplier.setName(request.name().trim());
        supplier.setContactName(request.contactName());
        supplier.setEmail(request.email());
        supplier.setPhone(request.phone());
        supplier.setAddress(request.address());
        if (request.status() != null) {
            supplier.setStatus(request.status().toUpperCase());
        }
        supplier.setNotes(request.notes());

        if (request.categoryIds() != null) {
            Set<ItemCategory> categories = new HashSet<>(categoryRepository.findAllById(request.categoryIds()));
            supplier.setCategories(categories);
        }

        return SupplierResponse.fromEntity(supplierRepository.save(supplier));
    }

    @Transactional
    public void delete(Long id) {
        if (!supplierRepository.existsById(id)) {
            throw new ResourceNotFoundException("Supplier not found with id: " + id);
        }
        supplierRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public SupplierPurchaseHistoryResponse getPurchaseHistory(Long supplierId) {
        Supplier supplier = supplierRepository.findById(supplierId)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found with id: " + supplierId));

        var orders = purchaseOrderRepository.findAll((root, query, cb) ->
                cb.equal(root.get("supplier").get("id"), supplierId)
        );

        BigDecimal totalSpend = orders.stream()
                .map(po -> po.getTotalAmount() != null ? po.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<SupplierPurchaseHistoryResponse.PurchaseOrderSummary> summaries = orders.stream()
                .map(po -> new SupplierPurchaseHistoryResponse.PurchaseOrderSummary(
                        po.getId(),
                        po.getPoNumber(),
                        po.getStatus() != null ? po.getStatus().name() : null,
                        po.getTotalAmount(),
                        po.getCreatedAt()
                ))
                .toList();

        return new SupplierPurchaseHistoryResponse(
                supplier.getId(),
                supplier.getName(),
                orders.size(),
                totalSpend,
                summaries
        );
    }
}
