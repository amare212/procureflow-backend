package com.example.procureflow.requisition.service;

import com.example.procureflow.approval.dto.ApprovalResponse;
import com.example.procureflow.approval.repository.ApprovalRepository;
import com.example.procureflow.common.dto.PageResponse;
import com.example.procureflow.common.exception.BusinessException;
import com.example.procureflow.common.exception.ResourceNotFoundException;
import com.example.procureflow.item.entity.Item;
import com.example.procureflow.item.repository.ItemRepository;
import com.example.procureflow.requisition.dto.RequisitionRequest;
import com.example.procureflow.requisition.dto.RequisitionResponse;
import com.example.procureflow.requisition.entity.Requisition;
import com.example.procureflow.requisition.entity.RequisitionItem;
import com.example.procureflow.requisition.entity.RequisitionStatus;
import com.example.procureflow.requisition.repository.RequisitionRepository;
import com.example.procureflow.role.entity.RoleName;
import com.example.procureflow.security.CurrentUser;
import com.example.procureflow.user.entity.AppUser;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.ArrayList;
import java.util.List;

@Service
public class RequisitionService {

    private final RequisitionRepository requisitionRepository;
    private final ItemRepository itemRepository;
    private final ApprovalRepository approvalRepository;
    private final RequisitionWorkflowService workflowService;
    private final CurrentUser currentUser;

    public RequisitionService(
            RequisitionRepository requisitionRepository,
            ItemRepository itemRepository,
            ApprovalRepository approvalRepository,
            RequisitionWorkflowService workflowService,
            CurrentUser currentUser
    ) {
        this.requisitionRepository = requisitionRepository;
        this.itemRepository = itemRepository;
        this.approvalRepository = approvalRepository;
        this.workflowService = workflowService;
        this.currentUser = currentUser;
    }

    @Transactional
    public RequisitionResponse create(RequisitionRequest request) {
        AppUser actor = currentUser.getUser();
        if (actor.getDepartment() == null) {
            throw new BusinessException("User must belong to a department to create requisitions");
        }

        Requisition req = new Requisition();
        long seq = requisitionRepository.getNextSequenceValue();
        String reqNumber = String.format("REQ-%d-%06d", Year.now().getValue(), seq);
        req.setRequisitionNumber(reqNumber);
        req.setEmployee(actor);
        req.setDepartment(actor.getDepartment());
        req.setJustification(request.justification().trim());
        req.setPriority(request.priority() != null ? request.priority().toUpperCase() : "MEDIUM");
        req.setRequiredDate(request.requiredDate());
        req.setStatus(RequisitionStatus.DRAFT);

        for (RequisitionRequest.RequisitionItemRequest itemReq : request.items()) {
            Item item = itemRepository.findById(itemReq.itemId())
                    .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + itemReq.itemId()));
            RequisitionItem reqItem = new RequisitionItem(item, itemReq.quantity(), itemReq.estimatedUnitPrice());
            req.addItem(reqItem);
        }

        Requisition saved = requisitionRepository.save(req);
        return RequisitionResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<RequisitionResponse> findAll(String search, RequisitionStatus status, Long departmentId, Pageable pageable) {
        AppUser actor = currentUser.getUser();
        RoleName role = actor.getRole().getName();

        Specification<Requisition> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Record-level role scoping
            if (role == RoleName.EMPLOYEE) {
                predicates.add(cb.equal(root.get("employee").get("id"), actor.getId()));
            } else if (role == RoleName.DEPARTMENT_MANAGER) {
                if (actor.getDepartment() != null) {
                    predicates.add(cb.equal(root.get("department").get("id"), actor.getDepartment().getId()));
                }
            } else if (role == RoleName.FINANCE_OFFICER) {
                predicates.add(root.get("status").in(
                        RequisitionStatus.FINANCE_APPROVAL,
                        RequisitionStatus.PROCUREMENT,
                        RequisitionStatus.ORDERED,
                        RequisitionStatus.RECEIVED,
                        RequisitionStatus.COMPLETED,
                        RequisitionStatus.REJECTED
                ));
            } else if (role == RoleName.PROCUREMENT_OFFICER) {
                predicates.add(root.get("status").in(
                        RequisitionStatus.PROCUREMENT,
                        RequisitionStatus.ORDERED,
                        RequisitionStatus.RECEIVED,
                        RequisitionStatus.COMPLETED
                ));
            } else if (role == RoleName.STORE_KEEPER) {
                predicates.add(root.get("status").in(
                        RequisitionStatus.ORDERED,
                        RequisitionStatus.RECEIVED,
                        RequisitionStatus.COMPLETED
                ));
            } // ADMIN sees all

            if (search != null && !search.trim().isEmpty()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("requisitionNumber")), pattern),
                        cb.like(cb.lower(root.get("justification")), pattern)
                ));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (departmentId != null) {
                predicates.add(cb.equal(root.get("department").get("id"), departmentId));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<RequisitionResponse> page = requisitionRepository.findAll(spec, pageable)
                .map(RequisitionResponse::fromEntity);
        return PageResponse.of(page);
    }

    @Transactional(readOnly = true)
    public RequisitionResponse findById(Long id) {
        Requisition req = getRequisitionWithAuthCheck(id);
        return RequisitionResponse.fromEntity(req);
    }

    @Transactional
    public RequisitionResponse update(Long id, RequisitionRequest request) {
        Requisition req = getRequisitionWithAuthCheck(id);
        AppUser actor = currentUser.getUser();

        if (!req.getEmployee().getId().equals(actor.getId()) && actor.getRole().getName() != RoleName.ADMIN) {
            throw new AccessDeniedException("Only the requester can modify this requisition");
        }

        if (req.getStatus() != RequisitionStatus.DRAFT) {
            throw new BusinessException("Cannot update requisition in status: " + req.getStatus() + ". Only DRAFT can be modified.");
        }

        req.setJustification(request.justification().trim());
        if (request.priority() != null) {
            req.setPriority(request.priority().toUpperCase());
        }
        req.setRequiredDate(request.requiredDate());

        // Update items
        req.getItems().clear();
        for (RequisitionRequest.RequisitionItemRequest itemReq : request.items()) {
            Item item = itemRepository.findById(itemReq.itemId())
                    .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + itemReq.itemId()));
            RequisitionItem reqItem = new RequisitionItem(item, itemReq.quantity(), itemReq.estimatedUnitPrice());
            req.addItem(reqItem);
        }

        return RequisitionResponse.fromEntity(requisitionRepository.save(req));
    }

    @Transactional
    public void delete(Long id) {
        Requisition req = getRequisitionWithAuthCheck(id);
        AppUser actor = currentUser.getUser();

        if (!req.getEmployee().getId().equals(actor.getId()) && actor.getRole().getName() != RoleName.ADMIN) {
            throw new AccessDeniedException("Only the requester can delete this requisition");
        }

        if (req.getStatus() != RequisitionStatus.DRAFT) {
            throw new BusinessException("Cannot delete requisition in status: " + req.getStatus() + ". Only DRAFT can be deleted.");
        }

        requisitionRepository.delete(req);
    }

    @Transactional
    public RequisitionResponse submit(Long id) {
        Requisition req = getRequisitionWithAuthCheck(id);
        AppUser actor = currentUser.getUser();
        Requisition submitted = workflowService.submit(req, actor);
        return RequisitionResponse.fromEntity(submitted);
    }

    @Transactional
    public RequisitionResponse approve(Long id, String comment) {
        Requisition req = getRequisitionWithAuthCheck(id);
        AppUser actor = currentUser.getUser();
        Requisition approved = workflowService.approve(req, actor, comment);
        return RequisitionResponse.fromEntity(approved);
    }

    @Transactional
    public RequisitionResponse reject(Long id, String reason) {
        Requisition req = getRequisitionWithAuthCheck(id);
        AppUser actor = currentUser.getUser();
        Requisition rejected = workflowService.reject(req, actor, reason);
        return RequisitionResponse.fromEntity(rejected);
    }

    @Transactional(readOnly = true)
    public List<ApprovalResponse> getApprovals(Long requisitionId) {
        getRequisitionWithAuthCheck(requisitionId);
        return approvalRepository.findByRequisitionIdOrderByCreatedAtAsc(requisitionId)
                .stream()
                .map(ApprovalResponse::fromEntity)
                .toList();
    }

    private Requisition getRequisitionWithAuthCheck(Long id) {
        Requisition req = requisitionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Requisition not found with id: " + id));

        AppUser actor = currentUser.getUser();
        RoleName role = actor.getRole().getName();

        if (role == RoleName.ADMIN) {
            return req;
        }

        if (role == RoleName.EMPLOYEE) {
            if (!req.getEmployee().getId().equals(actor.getId())) {
                throw new AccessDeniedException("Access denied: You can only view your own requisitions");
            }
        } else if (role == RoleName.DEPARTMENT_MANAGER) {
            if (actor.getDepartment() == null || !actor.getDepartment().getId().equals(req.getDepartment().getId())) {
                throw new AccessDeniedException("Access denied: You can only view requisitions in your department");
            }
        }

        return req;
    }
}
