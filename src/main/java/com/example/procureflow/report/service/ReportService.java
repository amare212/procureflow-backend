package com.example.procureflow.report.service;

import com.example.procureflow.department.entity.Department;
import com.example.procureflow.department.repository.DepartmentRepository;
import com.example.procureflow.inventory.entity.Inventory;
import com.example.procureflow.inventory.repository.InventoryRepository;
import com.example.procureflow.purchaseorder.entity.PurchaseOrder;
import com.example.procureflow.purchaseorder.entity.PurchaseOrderStatus;
import com.example.procureflow.purchaseorder.repository.PurchaseOrderRepository;
import com.example.procureflow.report.dto.*;
import com.example.procureflow.requisition.entity.Requisition;
import com.example.procureflow.requisition.entity.RequisitionStatus;
import com.example.procureflow.requisition.repository.RequisitionRepository;
import com.example.procureflow.role.entity.RoleName;
import com.example.procureflow.security.CurrentUser;
import com.example.procureflow.supplier.entity.Supplier;
import com.example.procureflow.supplier.repository.SupplierRepository;
import com.example.procureflow.user.entity.AppUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final RequisitionRepository requisitionRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final InventoryRepository inventoryRepository;
    private final DepartmentRepository departmentRepository;
    private final SupplierRepository supplierRepository;
    private final CurrentUser currentUser;

    public ReportService(
            RequisitionRepository requisitionRepository,
            PurchaseOrderRepository purchaseOrderRepository,
            InventoryRepository inventoryRepository,
            DepartmentRepository departmentRepository,
            SupplierRepository supplierRepository,
            CurrentUser currentUser
    ) {
        this.requisitionRepository = requisitionRepository;
        this.purchaseOrderRepository = purchaseOrderRepository;
        this.inventoryRepository = inventoryRepository;
        this.departmentRepository = departmentRepository;
        this.supplierRepository = supplierRepository;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getDashboardSummary() {
        AppUser actor = currentUser.getUser();
        RoleName role = actor.getRole().getName();
        Long deptId = (role == RoleName.DEPARTMENT_MANAGER && actor.getDepartment() != null)
                ? actor.getDepartment().getId() : null;

        List<Requisition> requisitions = requisitionRepository.findAll();
        if (deptId != null) {
            requisitions = requisitions.stream()
                    .filter(r -> r.getDepartment() != null && deptId.equals(r.getDepartment().getId()))
                    .toList();
        }

        long totalRequisitions = requisitions.size();
        long pendingApprovals = requisitions.stream()
                .filter(r -> r.getStatus() == RequisitionStatus.MANAGER_APPROVAL || r.getStatus() == RequisitionStatus.FINANCE_APPROVAL)
                .count();

        List<PurchaseOrder> orders = purchaseOrderRepository.findAll();
        if (deptId != null) {
            orders = orders.stream()
                    .filter(po -> po.getRequisition() != null && po.getRequisition().getDepartment() != null
                            && deptId.equals(po.getRequisition().getDepartment().getId()))
                    .toList();
        }

        long totalPOs = orders.size();
        long outstandingOrders = orders.stream()
                .filter(po -> po.getStatus() == PurchaseOrderStatus.ORDERED || po.getStatus() == PurchaseOrderStatus.PARTIALLY_RECEIVED)
                .count();

        BigDecimal totalSpend = orders.stream()
                .map(po -> po.getTotalAmount() != null ? po.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Inventory> inventoryList = inventoryRepository.findAll();
        long totalInventoryItems = inventoryList.size();
        long lowStockCount = inventoryList.stream()
                .filter(i -> i.getItem() != null && i.getQuantity() <= i.getItem().getReorderLevel())
                .count();

        return new DashboardSummaryResponse(
                totalRequisitions,
                pendingApprovals,
                totalPOs,
                outstandingOrders,
                totalInventoryItems,
                lowStockCount,
                totalSpend
        );
    }

    @Transactional(readOnly = true)
    public ExpenditureReportResponse getExpenditureReport(ReportFilter filter) {
        List<PurchaseOrder> orders = purchaseOrderRepository.findAll().stream()
                .filter(po -> filterDate(po.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate(), filter.from(), filter.to()))
                .filter(po -> filter.departmentId() == null || (po.getRequisition() != null && po.getRequisition().getDepartment() != null && filter.departmentId().equals(po.getRequisition().getDepartment().getId())))
                .filter(po -> filter.supplierId() == null || (po.getSupplier() != null && filter.supplierId().equals(po.getSupplier().getId())))
                .toList();

        BigDecimal totalSpend = orders.stream()
                .map(po -> po.getTotalAmount() != null ? po.getTotalAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Group by department
        Map<Long, List<PurchaseOrder>> byDept = orders.stream()
                .filter(po -> po.getRequisition() != null && po.getRequisition().getDepartment() != null)
                .collect(Collectors.groupingBy(po -> po.getRequisition().getDepartment().getId()));

        List<ExpenditureReportResponse.DepartmentSpend> deptSpends = new ArrayList<>();
        for (var entry : byDept.entrySet()) {
            Department d = entry.getValue().get(0).getRequisition().getDepartment();
            BigDecimal sum = entry.getValue().stream()
                    .map(po -> po.getTotalAmount() != null ? po.getTotalAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            deptSpends.add(new ExpenditureReportResponse.DepartmentSpend(d.getId(), d.getName(), sum, entry.getValue().size()));
        }

        // Group by supplier
        Map<Long, List<PurchaseOrder>> bySupp = orders.stream()
                .filter(po -> po.getSupplier() != null)
                .collect(Collectors.groupingBy(po -> po.getSupplier().getId()));

        List<ExpenditureReportResponse.SupplierSpend> suppSpends = new ArrayList<>();
        for (var entry : bySupp.entrySet()) {
            Supplier s = entry.getValue().get(0).getSupplier();
            BigDecimal sum = entry.getValue().stream()
                    .map(po -> po.getTotalAmount() != null ? po.getTotalAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            suppSpends.add(new ExpenditureReportResponse.SupplierSpend(s.getId(), s.getName(), sum, entry.getValue().size()));
        }

        return new ExpenditureReportResponse(
                totalSpend,
                filter.from(),
                filter.to(),
                deptSpends,
                suppSpends
        );
    }

    @Transactional(readOnly = true)
    public RequisitionReportResponse getRequisitionReport(ReportFilter filter) {
        List<Requisition> reqs = requisitionRepository.findAll().stream()
                .filter(r -> filterDate(r.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate(), filter.from(), filter.to()))
                .filter(r -> filter.departmentId() == null || (r.getDepartment() != null && filter.departmentId().equals(r.getDepartment().getId())))
                .toList();

        Map<String, Long> byStatus = reqs.stream()
                .collect(Collectors.groupingBy(r -> r.getStatus().name(), Collectors.counting()));

        Map<String, Long> byPriority = reqs.stream()
                .collect(Collectors.groupingBy(Requisition::getPriority, Collectors.counting()));

        Map<Long, List<Requisition>> byDept = reqs.stream()
                .filter(r -> r.getDepartment() != null)
                .collect(Collectors.groupingBy(r -> r.getDepartment().getId()));

        List<RequisitionReportResponse.DepartmentRequisitionStat> deptStats = new ArrayList<>();
        for (var entry : byDept.entrySet()) {
            Department d = entry.getValue().get(0).getDepartment();
            BigDecimal totalEst = entry.getValue().stream()
                    .map(Requisition::getEstimatedTotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            deptStats.add(new RequisitionReportResponse.DepartmentRequisitionStat(
                    d.getId(), d.getName(), entry.getValue().size(), totalEst
            ));
        }

        return new RequisitionReportResponse(reqs.size(), byStatus, byPriority, deptStats);
    }

    @Transactional(readOnly = true)
    public InventoryValuationReportResponse getInventoryValuationReport(ReportFilter filter) {
        List<Inventory> list = inventoryRepository.findAll().stream()
                .filter(inv -> filter.categoryId() == null || (inv.getItem() != null && inv.getItem().getCategory() != null && filter.categoryId().equals(inv.getItem().getCategory().getId())))
                .toList();

        long totalQty = 0;
        BigDecimal totalVal = BigDecimal.ZERO;
        List<InventoryValuationReportResponse.ItemValuationDetail> items = new ArrayList<>();

        for (Inventory inv : list) {
            if (inv.getItem() == null) continue;
            totalQty += inv.getQuantity();
            BigDecimal unitPrice = inv.getItem().getUnitPrice() != null ? inv.getItem().getUnitPrice() : BigDecimal.ZERO;
            BigDecimal itemTotal = unitPrice.multiply(BigDecimal.valueOf(inv.getQuantity()));
            totalVal = totalVal.add(itemTotal);

            boolean isLow = inv.getQuantity() <= inv.getItem().getReorderLevel();
            items.add(new InventoryValuationReportResponse.ItemValuationDetail(
                    inv.getItem().getId(),
                    inv.getItem().getSku(),
                    inv.getItem().getName(),
                    inv.getItem().getCategory() != null ? inv.getItem().getCategory().getName() : null,
                    inv.getQuantity(),
                    unitPrice,
                    itemTotal,
                    isLow
            ));
        }

        return new InventoryValuationReportResponse(list.size(), totalQty, totalVal, items);
    }

    @Transactional(readOnly = true)
    public SupplierReportResponse getSupplierReport(ReportFilter filter) {
        List<Supplier> suppliers = supplierRepository.findAll();
        List<PurchaseOrder> orders = purchaseOrderRepository.findAll();

        Map<Long, List<PurchaseOrder>> ordersBySupplier = orders.stream()
                .filter(po -> po.getSupplier() != null)
                .collect(Collectors.groupingBy(po -> po.getSupplier().getId()));

        List<SupplierReportResponse.SupplierStatDetail> details = new ArrayList<>();
        for (Supplier s : suppliers) {
            List<PurchaseOrder> sOrders = ordersBySupplier.getOrDefault(s.getId(), List.of());
            BigDecimal spend = sOrders.stream()
                    .map(po -> po.getTotalAmount() != null ? po.getTotalAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            details.add(new SupplierReportResponse.SupplierStatDetail(
                    s.getId(), s.getName(), s.getStatus(), sOrders.size(), spend
            ));
        }

        return new SupplierReportResponse(suppliers.size(), details);
    }

    @Transactional(readOnly = true)
    public DepartmentReportResponse getDepartmentReport(ReportFilter filter) {
        List<Department> departments = departmentRepository.findAll();
        List<PurchaseOrder> orders = purchaseOrderRepository.findAll();
        List<Requisition> requisitions = requisitionRepository.findAll();

        Map<Long, List<PurchaseOrder>> ordersByDept = orders.stream()
                .filter(po -> po.getRequisition() != null && po.getRequisition().getDepartment() != null)
                .collect(Collectors.groupingBy(po -> po.getRequisition().getDepartment().getId()));

        Map<Long, Long> reqCounts = requisitions.stream()
                .filter(r -> r.getDepartment() != null)
                .collect(Collectors.groupingBy(r -> r.getDepartment().getId(), Collectors.counting()));

        List<DepartmentReportResponse.DepartmentStatDetail> details = new ArrayList<>();
        for (Department d : departments) {
            List<PurchaseOrder> dOrders = ordersByDept.getOrDefault(d.getId(), List.of());
            BigDecimal spend = dOrders.stream()
                    .map(po -> po.getTotalAmount() != null ? po.getTotalAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal remaining = d.getBudget() != null ? d.getBudget().subtract(spend) : BigDecimal.ZERO;
            long count = reqCounts.getOrDefault(d.getId(), 0L);

            details.add(new DepartmentReportResponse.DepartmentStatDetail(
                    d.getId(), d.getName(), d.getCode(), d.getBudget(), spend, remaining, count
            ));
        }

        return new DepartmentReportResponse(departments.size(), details);
    }

    private boolean filterDate(LocalDate date, LocalDate from, LocalDate to) {
        if (from != null && date.isBefore(from)) return false;
        if (to != null && date.isAfter(to)) return false;
        return true;
    }
}
