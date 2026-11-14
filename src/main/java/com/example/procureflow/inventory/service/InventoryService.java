package com.example.procureflow.inventory.service;

import com.example.procureflow.common.dto.PageResponse;
import com.example.procureflow.common.exception.BusinessException;
import com.example.procureflow.common.exception.ResourceNotFoundException;
import com.example.procureflow.department.entity.Department;
import com.example.procureflow.department.repository.DepartmentRepository;
import com.example.procureflow.inventory.dto.InventoryResponse;
import com.example.procureflow.inventory.dto.StockAdjustmentRequest;
import com.example.procureflow.inventory.dto.StockIssueRequest;
import com.example.procureflow.inventory.entity.Inventory;
import com.example.procureflow.inventory.repository.InventoryRepository;
import com.example.procureflow.item.entity.Item;
import com.example.procureflow.item.repository.ItemRepository;
import com.example.procureflow.security.CurrentUser;
import com.example.procureflow.stockmovement.entity.MovementType;
import com.example.procureflow.stockmovement.entity.StockMovement;
import com.example.procureflow.stockmovement.repository.StockMovementRepository;
import com.example.procureflow.user.entity.AppUser;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ItemRepository itemRepository;
    private final DepartmentRepository departmentRepository;
    private final StockMovementRepository stockMovementRepository;
    private final CurrentUser currentUser;

    public InventoryService(
            InventoryRepository inventoryRepository,
            ItemRepository itemRepository,
            DepartmentRepository departmentRepository,
            StockMovementRepository stockMovementRepository,
            CurrentUser currentUser
    ) {
        this.inventoryRepository = inventoryRepository;
        this.itemRepository = itemRepository;
        this.departmentRepository = departmentRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public PageResponse<InventoryResponse> findAll(String search, Boolean lowStock, Pageable pageable) {
        Specification<Inventory> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (search != null && !search.trim().isEmpty()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("item").get("name")), pattern),
                        cb.like(cb.lower(root.get("item").get("sku")), pattern),
                        cb.like(cb.lower(root.get("location")), pattern)
                ));
            }
            if (Boolean.TRUE.equals(lowStock)) {
                predicates.add(cb.lessThanOrEqualTo(root.get("quantity"), root.get("item").get("reorderLevel")));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<InventoryResponse> page = inventoryRepository.findAll(spec, pageable)
                .map(InventoryResponse::fromEntity);
        return PageResponse.of(page);
    }

    @Transactional(readOnly = true)
    public PageResponse<InventoryResponse> findLowStock(Pageable pageable) {
        Page<InventoryResponse> page = inventoryRepository.findLowStock(pageable)
                .map(InventoryResponse::fromEntity);
        return PageResponse.of(page);
    }

    @Transactional(readOnly = true)
    public InventoryResponse findByItemId(Long itemId) {
        Inventory inv = inventoryRepository.findByItemId(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for item id: " + itemId));
        return InventoryResponse.fromEntity(inv);
    }

    /**
     * Issues stock to a department with pessimistic locking to prevent race conditions.
     */
    @Transactional
    public InventoryResponse issueStock(StockIssueRequest request) {
        Item item = itemRepository.findById(request.itemId())
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + request.itemId()));

        Department department = departmentRepository.findById(request.departmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + request.departmentId()));

        AppUser actor = currentUser.getUser();

        // Pessimistic write lock on inventory row
        Inventory inventory = inventoryRepository.findByItemIdWithLock(item.getId())
                .orElseThrow(() -> new BusinessException("No inventory record found for item: " + item.getName()));

        int requestedQty = request.quantity();
        int currentQty = inventory.getQuantity();

        if (currentQty < requestedQty) {
            throw new BusinessException(
                    "Insufficient stock: Requested " + requestedQty + " " + item.getUnit() +
                    ", but only " + currentQty + " available in stock for item: " + item.getName()
            );
        }

        inventory.setQuantity(currentQty - requestedQty);
        Inventory saved = inventoryRepository.save(inventory);

        // Record stock movement (append-only)
        StockMovement movement = new StockMovement();
        movement.setItem(item);
        movement.setMovementType(MovementType.ISSUE);
        movement.setQuantity(-requestedQty);
        movement.setReferenceType("DEPARTMENT_ISSUE");
        movement.setDepartment(department);
        movement.setPerformedBy(actor);
        movement.setNote("Issued to " + request.recipientName() + " (" + department.getName() + "): " + request.reason());
        stockMovementRepository.save(movement);

        return InventoryResponse.fromEntity(saved);
    }

    /**
     * Adjusts inventory stock with pessimistic locking.
     */
    @Transactional
    public InventoryResponse adjustStock(StockAdjustmentRequest request) {
        Item item = itemRepository.findById(request.itemId())
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + request.itemId()));

        AppUser actor = currentUser.getUser();

        Inventory inventory = inventoryRepository.findByItemIdWithLock(item.getId())
                .orElseGet(() -> {
                    Inventory newInv = new Inventory();
                    newInv.setItem(item);
                    newInv.setQuantity(0);
                    return newInv;
                });

        int currentQty = inventory.getQuantity();
        int newQty = request.adjustedQuantity();
        int delta = newQty - currentQty;

        inventory.setQuantity(newQty);
        Inventory saved = inventoryRepository.save(inventory);

        if (delta != 0) {
            StockMovement movement = new StockMovement();
            movement.setItem(item);
            movement.setMovementType(MovementType.ADJUSTMENT);
            movement.setQuantity(delta);
            movement.setReferenceType("MANUAL_ADJUSTMENT");
            movement.setPerformedBy(actor);
            movement.setNote("Stock adjustment (" + (delta > 0 ? "+" : "") + delta + "): " + request.reason());
            stockMovementRepository.save(movement);
        }

        return InventoryResponse.fromEntity(saved);
    }
}
