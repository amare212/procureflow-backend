package com.example.procureflow.requisition;

import com.example.procureflow.approval.repository.ApprovalRepository;
import com.example.procureflow.common.exception.BusinessException;
import com.example.procureflow.common.exception.InvalidStateTransitionException;
import com.example.procureflow.department.entity.Department;
import com.example.procureflow.department.repository.DepartmentRepository;
import com.example.procureflow.item.entity.Item;
import com.example.procureflow.requisition.entity.Requisition;
import com.example.procureflow.requisition.entity.RequisitionItem;
import com.example.procureflow.requisition.entity.RequisitionStatus;
import com.example.procureflow.requisition.repository.RequisitionRepository;
import com.example.procureflow.requisition.service.RequisitionWorkflowService;
import com.example.procureflow.role.entity.Role;
import com.example.procureflow.role.entity.RoleName;
import com.example.procureflow.user.entity.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RequisitionWorkflowServiceTest {

    @Mock
    private RequisitionRepository requisitionRepository;

    @Mock
    private ApprovalRepository approvalRepository;

    @Mock
    private DepartmentRepository departmentRepository;

    @InjectMocks
    private RequisitionWorkflowService workflowService;

    private Department dept;
    private AppUser employee;
    private AppUser manager;
    private AppUser finance;
    private Requisition requisition;

    @BeforeEach
    void setUp() {
        dept = new Department("IT", "IT", BigDecimal.valueOf(50000));
        dept.setId(1L);

        Role empRole = new Role(RoleName.EMPLOYEE);
        empRole.setId(1L);
        employee = new AppUser();
        employee.setId(10L);
        employee.setRole(empRole);
        employee.setDepartment(dept);

        Role mgrRole = new Role(RoleName.DEPARTMENT_MANAGER);
        mgrRole.setId(2L);
        manager = new AppUser();
        manager.setId(20L);
        manager.setRole(mgrRole);
        manager.setDepartment(dept);

        Role finRole = new Role(RoleName.FINANCE_OFFICER);
        finRole.setId(3L);
        finance = new AppUser();
        finance.setId(30L);
        finance.setRole(finRole);

        requisition = new Requisition();
        requisition.setId(100L);
        requisition.setEmployee(employee);
        requisition.setDepartment(dept);
        requisition.setStatus(RequisitionStatus.DRAFT);

        Item item = new Item();
        item.setId(5L);
        item.setUnitPrice(BigDecimal.valueOf(100));
        RequisitionItem reqItem = new RequisitionItem(item, 2, BigDecimal.valueOf(100));
        requisition.addItem(reqItem);

        when(requisitionRepository.save(any(Requisition.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void testSubmitRequisitionSuccess() {
        Requisition result = workflowService.submit(requisition, employee);
        assertEquals(RequisitionStatus.MANAGER_APPROVAL, result.getStatus());
    }

    @Test
    void testSubmitByOtherUserFails() {
        AppUser otherUser = new AppUser();
        otherUser.setId(99L);
        assertThrows(AccessDeniedException.class, () -> workflowService.submit(requisition, otherUser));
    }

    @Test
    void testManagerApproveSuccess() {
        requisition.setStatus(RequisitionStatus.MANAGER_APPROVAL);
        Requisition result = workflowService.approve(requisition, manager, "Approved for business need");

        assertEquals(RequisitionStatus.FINANCE_APPROVAL, result.getStatus());
        verify(approvalRepository, times(1)).save(any());
    }

    @Test
    void testRequesterCannotApproveOwnRequisition() {
        requisition.setStatus(RequisitionStatus.MANAGER_APPROVAL);
        assertThrows(AccessDeniedException.class, () -> workflowService.approve(requisition, employee, "self-approve"));
    }

    @Test
    void testManagerRejectRequiresReason() {
        requisition.setStatus(RequisitionStatus.MANAGER_APPROVAL);
        assertThrows(BusinessException.class, () -> workflowService.reject(requisition, manager, "  "));
    }

    @Test
    void testFinanceApproveBudgetCheckSuccess() {
        requisition.setStatus(RequisitionStatus.FINANCE_APPROVAL);
        Requisition result = workflowService.approve(requisition, finance, "Budget OK");

        assertEquals(RequisitionStatus.PROCUREMENT, result.getStatus());
    }

    @Test
    void testFinanceApproveBudgetExceededFails() {
        dept.setBudget(BigDecimal.valueOf(50)); // Estimated total is 200
        requisition.setStatus(RequisitionStatus.FINANCE_APPROVAL);

        assertThrows(BusinessException.class, () -> workflowService.approve(requisition, finance, "Budget"));
    }

    @Test
    void testInvalidTransitionThrowsConflict() {
        requisition.setStatus(RequisitionStatus.COMPLETED);
        assertThrows(InvalidStateTransitionException.class, () -> workflowService.approve(requisition, manager, "test"));
    }
}
