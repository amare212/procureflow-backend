package com.example.procureflow.goodsreceipt;

import com.example.procureflow.common.exception.BusinessException;
import com.example.procureflow.goodsreceipt.dto.GoodsReceiptRequest;
import com.example.procureflow.goodsreceipt.dto.GoodsReceiptResponse;
import com.example.procureflow.goodsreceipt.entity.GoodsReceipt;
import com.example.procureflow.goodsreceipt.repository.GoodsReceiptRepository;
import com.example.procureflow.goodsreceipt.service.GoodsReceiptService;
import com.example.procureflow.inventory.entity.Inventory;
import com.example.procureflow.inventory.repository.InventoryRepository;
import com.example.procureflow.item.entity.Item;
import com.example.procureflow.purchaseorder.entity.PurchaseOrder;
import com.example.procureflow.purchaseorder.entity.PurchaseOrderItem;
import com.example.procureflow.purchaseorder.entity.PurchaseOrderStatus;
import com.example.procureflow.purchaseorder.repository.PurchaseOrderRepository;
import com.example.procureflow.requisition.entity.Requisition;
import com.example.procureflow.requisition.service.RequisitionWorkflowService;
import com.example.procureflow.security.CurrentUser;
import com.example.procureflow.stockmovement.repository.StockMovementRepository;
import com.example.procureflow.user.entity.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoodsReceiptServiceTest {

    @Mock
    private GoodsReceiptRepository goodsReceiptRepository;

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private StockMovementRepository stockMovementRepository;

    @Mock
    private RequisitionWorkflowService workflowService;

    @Mock
    private CurrentUser currentUser;

    @InjectMocks
    private GoodsReceiptService goodsReceiptService;

    private PurchaseOrder po;
    private PurchaseOrderItem poItem;
    private Item item;
    private AppUser storeKeeper;

    @BeforeEach
    void setUp() {
        storeKeeper = new AppUser();
        storeKeeper.setId(50L);
        storeKeeper.setFullName("Marcus Storekeeper");

        item = new Item();
        item.setId(1L);
        item.setName("Laptop");
        item.setUnitPrice(BigDecimal.valueOf(1000));

        po = new PurchaseOrder();
        po.setId(10L);
        po.setPoNumber("PO-2026-000001");
        po.setStatus(PurchaseOrderStatus.ORDERED);
        po.setRequisition(new Requisition());

        poItem = new PurchaseOrderItem(item, 5, BigDecimal.valueOf(1000));
        poItem.setId(101L);
        po.addItem(poItem);
    }

    @Test
    void testReceiveGoodsSuccessfullyUpdatesInventoryAndPO() {
        when(goodsReceiptRepository.findByIdempotencyKey("key-1")).thenReturn(Optional.empty());
        when(purchaseOrderRepository.findByIdWithLock(10L)).thenReturn(Optional.of(po));
        when(currentUser.getUser()).thenReturn(storeKeeper);
        when(goodsReceiptRepository.getNextSequenceValue()).thenReturn(1L);

        Inventory inv = new Inventory(item, 10, "A-1");
        when(inventoryRepository.findByItemIdWithLock(1L)).thenReturn(Optional.of(inv));
        when(goodsReceiptRepository.save(any(GoodsReceipt.class))).thenAnswer(invocation -> {
            GoodsReceipt gr = invocation.getArgument(0);
            gr.setId(200L);
            return gr;
        });

        GoodsReceiptRequest req = new GoodsReceiptRequest(
                "DN-12345",
                "Delivered in good condition",
                true,
                List.of(new GoodsReceiptRequest.ReceiptItemRequest(101L, 5))
        );

        GoodsReceiptResponse response = goodsReceiptService.receive(10L, "key-1", req);

        assertNotNull(response);
        assertEquals(5, poItem.getReceivedQuantity());
        assertEquals(PurchaseOrderStatus.RECEIVED, po.getStatus());
        assertEquals(15, inv.getQuantity());
        verify(stockMovementRepository, times(1)).save(any());
        verify(workflowService, times(1)).receiptComplete(any());
    }

    @Test
    void testDuplicateIdempotencyKeyReturnsOriginalWithoutAddingStock() {
        GoodsReceipt existing = new GoodsReceipt();
        existing.setId(300L);
        existing.setReceiptNumber("GRN-2026-000001");
        existing.setIdempotencyKey("duplicate-key");
        existing.setPurchaseOrder(po);
        existing.setReceivedBy(storeKeeper);

        when(goodsReceiptRepository.findByIdempotencyKey("duplicate-key")).thenReturn(Optional.of(existing));

        GoodsReceiptRequest req = new GoodsReceiptRequest(
                "DN-12345", "Notes", true,
                List.of(new GoodsReceiptRequest.ReceiptItemRequest(101L, 5))
        );

        GoodsReceiptResponse response = goodsReceiptService.receive(10L, "duplicate-key", req);

        assertEquals("GRN-2026-000001", response.receiptNumber());
        verify(purchaseOrderRepository, never()).findByIdWithLock(any());
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void testQuantityGuardPreventsOverReceipt() {
        when(goodsReceiptRepository.findByIdempotencyKey("key-2")).thenReturn(Optional.empty());
        when(purchaseOrderRepository.findByIdWithLock(10L)).thenReturn(Optional.of(po));

        // Ordered is 5, but trying to receive 6
        GoodsReceiptRequest req = new GoodsReceiptRequest(
                "DN-9999", "Notes", true,
                List.of(new GoodsReceiptRequest.ReceiptItemRequest(101L, 6))
        );

        assertThrows(BusinessException.class, () -> goodsReceiptService.receive(10L, "key-2", req));
        verify(inventoryRepository, never()).save(any());
    }

    @Test
    void testCannotReceiveNonOpenPO() {
        po.setStatus(PurchaseOrderStatus.COMPLETED);
        when(goodsReceiptRepository.findByIdempotencyKey("key-3")).thenReturn(Optional.empty());
        when(purchaseOrderRepository.findByIdWithLock(10L)).thenReturn(Optional.of(po));

        GoodsReceiptRequest req = new GoodsReceiptRequest(
                "DN-8888", "Notes", true,
                List.of(new GoodsReceiptRequest.ReceiptItemRequest(101L, 2))
        );

        assertThrows(BusinessException.class, () -> goodsReceiptService.receive(10L, "key-3", req));
    }
}
