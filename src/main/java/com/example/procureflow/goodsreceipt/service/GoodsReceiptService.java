package com.example.procureflow.goodsreceipt.service;

import com.example.procureflow.common.dto.PageResponse;
import com.example.procureflow.common.exception.BusinessException;
import com.example.procureflow.common.exception.ResourceNotFoundException;
import com.example.procureflow.goodsreceipt.dto.GoodsReceiptRequest;
import com.example.procureflow.goodsreceipt.dto.GoodsReceiptResponse;
import com.example.procureflow.goodsreceipt.entity.GoodsReceipt;
import com.example.procureflow.goodsreceipt.entity.GoodsReceiptItem;
import com.example.procureflow.goodsreceipt.repository.GoodsReceiptRepository;
import com.example.procureflow.inventory.entity.Inventory;
import com.example.procureflow.inventory.repository.InventoryRepository;
import com.example.procureflow.purchaseorder.entity.PurchaseOrder;
import com.example.procureflow.purchaseorder.entity.PurchaseOrderItem;
import com.example.procureflow.purchaseorder.entity.PurchaseOrderStatus;
import com.example.procureflow.purchaseorder.repository.PurchaseOrderRepository;
import com.example.procureflow.requisition.service.RequisitionWorkflowService;
import com.example.procureflow.security.CurrentUser;
import com.example.procureflow.stockmovement.entity.MovementType;
import com.example.procureflow.stockmovement.entity.StockMovement;
import com.example.procureflow.stockmovement.repository.StockMovementRepository;
import com.example.procureflow.user.entity.AppUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class GoodsReceiptService {

    private static final Logger log = LoggerFactory.getLogger(GoodsReceiptService.class);

    private final GoodsReceiptRepository goodsReceiptRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final InventoryRepository inventoryRepository;
    private final StockMovementRepository stockMovementRepository;
    private final RequisitionWorkflowService workflowService;
    private final CurrentUser currentUser;

    public GoodsReceiptService(
            GoodsReceiptRepository goodsReceiptRepository,
            PurchaseOrderRepository purchaseOrderRepository,
            InventoryRepository inventoryRepository,
            StockMovementRepository stockMovementRepository,
            RequisitionWorkflowService workflowService,
            CurrentUser currentUser
    ) {
        this.goodsReceiptRepository = goodsReceiptRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.inventoryRepository = inventoryRepository;
        this.stockMovementRepository = stockMovementRepository;
        this.workflowService = workflowService;
        this.currentUser = currentUser;
    }

    /**
     * Critical acceptance scenario implementation with 4 layers of protection:
     * 1. Single transaction (@Transactional)
     * 2. Pessimistic row locking on PurchaseOrder
     * 3. Idempotency key protection (returns original receipt if duplicate key)
     * 4. Quantity guards (rejects over-receipt and closed POs)
     */
    @Transactional
    public GoodsReceiptResponse receive(Long purchaseOrderId, String idempotencyKey, GoodsReceiptRequest request) {
        // Layer 3: Idempotency check before doing any mutation
        if (idempotencyKey != null && !idempotencyKey.trim().isEmpty()) {
            Optional<GoodsReceipt> existingByKey = goodsReceiptRepository.findByIdempotencyKey(idempotencyKey.trim());
            if (existingByKey.isPresent()) {
                log.info("Idempotent duplicate goods receipt request detected for key: {}", idempotencyKey);
                return GoodsReceiptResponse.fromEntity(existingByKey.get());
            }
        }

        // Layer 2: Load PurchaseOrder with PESSIMISTIC_WRITE lock
        PurchaseOrder po = purchaseOrderRepository.findByIdWithLock(purchaseOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase order not found with id: " + purchaseOrderId));

        // Layer 4a: PO status guard
        if (po.getStatus() != PurchaseOrderStatus.ORDERED && po.getStatus() != PurchaseOrderStatus.PARTIALLY_RECEIVED) {
            throw new BusinessException(
                    "Cannot receive goods for purchase order in status: " + po.getStatus() +
                    ". Only ORDERED or PARTIALLY_RECEIVED purchase orders can be received."
            );
        }

        // Check if delivery note was already recorded for this PO
        if (request.deliveryNoteNumber() != null && !request.deliveryNoteNumber().trim().isEmpty()) {
            Optional<GoodsReceipt> existingByNote = goodsReceiptRepository.findByPurchaseOrderIdAndDeliveryNoteNumber(
                    purchaseOrderId, request.deliveryNoteNumber().trim()
            );
            if (existingByNote.isPresent()) {
                throw new BusinessException(
                        "Delivery note '" + request.deliveryNoteNumber() + "' has already been recorded for this purchase order"
                );
            }
        }

        // Map PO items by id
        Map<Long, PurchaseOrderItem> poItemMap = po.getItems().stream()
                .collect(Collectors.toMap(PurchaseOrderItem::getId, item -> item));

        AppUser actor = currentUser.getUser();

        long receiptSeq = goodsReceiptRepository.getNextSequenceValue();
        String receiptNumber = String.format("GRN-%d-%06d", Year.now().getValue(), receiptSeq);

        GoodsReceipt receipt = new GoodsReceipt();
        receipt.setReceiptNumber(receiptNumber);
        receipt.setPurchaseOrder(po);
        receipt.setIdempotencyKey(idempotencyKey != null ? idempotencyKey.trim() : null);
        receipt.setDeliveryNoteNumber(request.deliveryNoteNumber() != null ? request.deliveryNoteNumber().trim() : null);
        receipt.setReceivedBy(actor);
        receipt.setVerified(request.verified() != null ? request.verified() : true);
        receipt.setNotes(request.notes());

        for (GoodsReceiptRequest.ReceiptItemRequest itemReq : request.items()) {
            PurchaseOrderItem poItem = poItemMap.get(itemReq.purchaseOrderItemId());
            if (poItem == null) {
                throw new BusinessException(
                        "Purchase order item with ID " + itemReq.purchaseOrderItemId() + " does not belong to purchase order " + po.getPoNumber()
                );
            }

            // Layer 4b: Quantity guards
            int newQuantity = itemReq.quantityReceived();
            int currentReceived = poItem.getReceivedQuantity();
            int ordered = poItem.getQuantity();

            if (currentReceived + newQuantity > ordered) {
                throw new BusinessException(
                        "Quantity guard violation: Received quantity (" + (currentReceived + newQuantity) +
                        ") would exceed ordered quantity (" + ordered + ") for item: " + poItem.getItem().getName()
                );
            }

            // Update received quantity on PO item
            poItem.setReceivedQuantity(currentReceived + newQuantity);

            // Add receipt line item
            GoodsReceiptItem receiptItem = new GoodsReceiptItem(poItem, poItem.getItem(), newQuantity);
            receipt.addItem(receiptItem);

            // Update inventory with pessimistic lock
            Long itemId = poItem.getItem().getId();
            Inventory inventory = inventoryRepository.findByItemIdWithLock(itemId)
                    .orElseGet(() -> {
                        Inventory newInv = new Inventory();
                        newInv.setItem(poItem.getItem());
                        newInv.setQuantity(0);
                        return newInv;
                    });

            inventory.setQuantity(inventory.getQuantity() + newQuantity);
            inventoryRepository.save(inventory);

            // Record stock movement (append-only audit log)
            StockMovement movement = new StockMovement();
            movement.setItem(poItem.getItem());
            movement.setMovementType(MovementType.RECEIPT);
            movement.setQuantity(newQuantity);
            movement.setReferenceType("GOODS_RECEIPT");
            movement.setReferenceId(po.getId());
            movement.setPerformedBy(actor);
            movement.setNote("Goods receipt " + receiptNumber + " for PO " + po.getPoNumber());
            stockMovementRepository.save(movement);
        }

        // Determine if PO is fully received or partially received
        boolean allReceived = po.getItems().stream()
                .allMatch(item -> item.getReceivedQuantity() >= item.getQuantity());

        if (allReceived) {
            po.setStatus(PurchaseOrderStatus.RECEIVED);
            // Complete receipt on requisition workflow
            workflowService.receiptComplete(po.getRequisition());
        } else {
            po.setStatus(PurchaseOrderStatus.PARTIALLY_RECEIVED);
        }

        purchaseOrderRepository.save(po);
        GoodsReceipt saved = goodsReceiptRepository.save(receipt);

        return GoodsReceiptResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<GoodsReceiptResponse> findAll(Pageable pageable) {
        Page<GoodsReceiptResponse> page = goodsReceiptRepository.findAll(pageable)
                .map(GoodsReceiptResponse::fromEntity);
        return PageResponse.of(page);
    }

    @Transactional(readOnly = true)
    public GoodsReceiptResponse findById(Long id) {
        GoodsReceipt gr = goodsReceiptRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Goods receipt not found with id: " + id));
        return GoodsReceiptResponse.fromEntity(gr);
    }
}
