package com.example.procureflow.inventory;

import com.example.procureflow.common.exception.BusinessException;
import com.example.procureflow.department.entity.Department;
import com.example.procureflow.department.repository.DepartmentRepository;
import com.example.procureflow.inventory.dto.StockIssueRequest;
import com.example.procureflow.inventory.entity.Inventory;
import com.example.procureflow.inventory.repository.InventoryRepository;
import com.example.procureflow.inventory.service.InventoryService;
import com.example.procureflow.item.entity.Item;
import com.example.procureflow.item.repository.ItemRepository;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryConcurrencyTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private StockMovementRepository stockMovementRepository;

    @Mock
    private CurrentUser currentUser;

    @InjectMocks
    private InventoryService inventoryService;

    private Item item;
    private Department dept;
    private AppUser actor;

    @BeforeEach
    void setUp() {
        item = new Item();
        item.setId(1L);
        item.setName("Keyboard");
        item.setUnit("pcs");

        dept = new Department("IT", "IT", BigDecimal.valueOf(10000));
        dept.setId(10L);

        actor = new AppUser();
        actor.setId(1L);
        actor.setFullName("Marcus Storekeeper");
    }

    @Test
    void testIssueStockInsufficientQuantityFails() {
        Inventory inv = new Inventory(item, 5, "Shelf A");
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(departmentRepository.findById(10L)).thenReturn(Optional.of(dept));
        when(currentUser.getUser()).thenReturn(actor);
        when(inventoryRepository.findByItemIdWithLock(1L)).thenReturn(Optional.of(inv));

        StockIssueRequest request = new StockIssueRequest(1L, 10, 10L, "Alice", "Need keyboards");

        assertThrows(BusinessException.class, () -> inventoryService.issueStock(request));
        assertEquals(5, inv.getQuantity()); // unchanged
        verify(stockMovementRepository, never()).save(any());
    }

    @Test
    void testIssueStockSuccessDeductsBalance() {
        Inventory inv = new Inventory(item, 15, "Shelf A");
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(departmentRepository.findById(10L)).thenReturn(Optional.of(dept));
        when(currentUser.getUser()).thenReturn(actor);
        when(inventoryRepository.findByItemIdWithLock(1L)).thenReturn(Optional.of(inv));
        when(inventoryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        StockIssueRequest request = new StockIssueRequest(1L, 5, 10L, "Alice", "Need keyboards");

        var response = inventoryService.issueStock(request);

        assertEquals(10, inv.getQuantity());
        assertEquals(10, response.quantity());
        verify(stockMovementRepository, times(1)).save(any());
    }
}
