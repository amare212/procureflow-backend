package com.example.procureflow.requisition.service;

import com.example.procureflow.approval.entity.Approval;
import com.example.procureflow.approval.entity.ApprovalDecision;
import com.example.procureflow.approval.repository.ApprovalRepository;
import com.example.procureflow.common.exception.BusinessException;
import com.example.procureflow.common.exception.InvalidStateTransitionException;
import com.example.procureflow.department.entity.Department;
import com.example.procureflow.department.repository.DepartmentRepository;
import com.example.procureflow.requisition.entity.Requisition;
import com.example.procureflow.requisition.entity.RequisitionStatus;
import com.example.procureflow.requisition.repository.RequisitionRepository;
import com.example.procureflow.role.entity.RoleName;
import com.example.procureflow.user.entity.AppUser;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class RequisitionWorkflowService {

    private final RequisitionRepository requisitionRepository;
    private final ApprovalRepository approvalRepository;
    private final DepartmentRepository departmentRepository;

    public RequisitionWorkflowService(
            RequisitionRepository requisitionRepository,
            ApprovalRepository approvalRepository,
            DepartmentRepository departmentRepository
    ) {
        this.requisitionRepository = requisitionRepository;
        this.approvalRepository = approvalRepository;
        this.departmentRepository = departmentRepository;
    }

    @Transactional
    public Requisition submit(Requisition requisition, AppUser actor) {
        if (requisition.getStatus() != RequisitionStatus.DRAFT) {
            throw new InvalidStateTransitionException(
                    "Cannot submit requisition in status: " + requisition.getStatus() + ". Only DRAFT can be submitted."
            );
        }

        if (!requisition.getEmployee().getId().equals(actor.getId())) {
            throw new AccessDeniedException("Only the requester can submit the requisition");
        }

        if (requisition.getItems() == null || requisition.getItems().isEmpty()) {
            throw new BusinessException("Cannot submit a requisition without any line items");
        }

        // DRAFT -> SUBMITTED -> immediately moves to MANAGER_APPROVAL
        requisition.setStatus(RequisitionStatus.MANAGER_APPROVAL);
        return requisitionRepository.save(requisition);
    }

    @Transactional
    public Requisition approve(Requisition requisition, AppUser actor, String comment) {
        RequisitionStatus currentStatus = requisition.getStatus();
        RoleName actorRole = actor.getRole().getName();

        if (currentStatus == RequisitionStatus.MANAGER_APPROVAL) {
            // Manager Approval stage
            if (actorRole != RoleName.DEPARTMENT_MANAGER && actorRole != RoleName.ADMIN) {
                throw new AccessDeniedException("Only a DEPARTMENT_MANAGER can perform manager approval");
            }

            // Cannot be the requester
            if (requisition.getEmployee().getId().equals(actor.getId())) {
                throw new AccessDeniedException("Department manager cannot approve their own requisition");
            }

            // Must be manager of the same department
            if (actor.getDepartment() == null || !actor.getDepartment().getId().equals(requisition.getDepartment().getId())) {
                throw new AccessDeniedException("Manager can only approve requisitions from their own department");
            }

            requisition.setStatus(RequisitionStatus.FINANCE_APPROVAL);
            recordApproval(requisition, "MANAGER_APPROVAL", ApprovalDecision.APPROVED, actor, comment);

        } else if (currentStatus == RequisitionStatus.FINANCE_APPROVAL) {
            // Finance Approval stage
            if (actorRole != RoleName.FINANCE_OFFICER && actorRole != RoleName.ADMIN) {
                throw new AccessDeniedException("Only a FINANCE_OFFICER can perform finance approval");
            }

            // Budget availability check
            Department dept = requisition.getDepartment();
            BigDecimal estimatedTotal = requisition.getEstimatedTotal();
            if (dept.getBudget() != null && dept.getBudget().compareTo(estimatedTotal) < 0) {
                throw new BusinessException(
                        "Budget check failed: Department budget (" + dept.getBudget() +
                        ") is insufficient for estimated total (" + estimatedTotal + ")"
                );
            }

            requisition.setStatus(RequisitionStatus.PROCUREMENT);
            recordApproval(requisition, "FINANCE_APPROVAL", ApprovalDecision.APPROVED, actor, comment);

        } else {
            throw new InvalidStateTransitionException(
                    "Cannot approve requisition in status: " + currentStatus +
                    ". Approvals only accepted in MANAGER_APPROVAL or FINANCE_APPROVAL stage."
            );
        }

        return requisitionRepository.save(requisition);
    }

    @Transactional
    public Requisition reject(Requisition requisition, AppUser actor, String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new BusinessException("Rejection reason is required");
        }

        RequisitionStatus currentStatus = requisition.getStatus();
        RoleName actorRole = actor.getRole().getName();

        if (currentStatus == RequisitionStatus.MANAGER_APPROVAL) {
            if (actorRole != RoleName.DEPARTMENT_MANAGER && actorRole != RoleName.ADMIN) {
                throw new AccessDeniedException("Only a DEPARTMENT_MANAGER can reject at manager approval stage");
            }
            if (actor.getDepartment() == null || !actor.getDepartment().getId().equals(requisition.getDepartment().getId())) {
                throw new AccessDeniedException("Manager can only reject requisitions from their own department");
            }
            requisition.setStatus(RequisitionStatus.REJECTED);
            recordApproval(requisition, "MANAGER_APPROVAL", ApprovalDecision.REJECTED, actor, reason.trim());

        } else if (currentStatus == RequisitionStatus.FINANCE_APPROVAL) {
            if (actorRole != RoleName.FINANCE_OFFICER && actorRole != RoleName.ADMIN) {
                throw new AccessDeniedException("Only a FINANCE_OFFICER can reject at finance approval stage");
            }
            requisition.setStatus(RequisitionStatus.REJECTED);
            recordApproval(requisition, "FINANCE_APPROVAL", ApprovalDecision.REJECTED, actor, reason.trim());

        } else {
            throw new InvalidStateTransitionException(
                    "Cannot reject requisition in status: " + currentStatus +
                    ". Rejection only allowed in MANAGER_APPROVAL or FINANCE_APPROVAL stage."
            );
        }

        return requisitionRepository.save(requisition);
    }

    @Transactional
    public Requisition issuePo(Requisition requisition) {
        if (requisition.getStatus() != RequisitionStatus.PROCUREMENT) {
            throw new InvalidStateTransitionException(
                    "Cannot issue PO for requisition in status: " + requisition.getStatus() +
                    ". Expected status: PROCUREMENT."
            );
        }
        requisition.setStatus(RequisitionStatus.ORDERED);
        return requisitionRepository.save(requisition);
    }

    @Transactional
    public Requisition receiptComplete(Requisition requisition) {
        if (requisition.getStatus() != RequisitionStatus.ORDERED) {
            throw new InvalidStateTransitionException(
                    "Cannot mark received for requisition in status: " + requisition.getStatus() +
                    ". Expected status: ORDERED."
            );
        }
        requisition.setStatus(RequisitionStatus.RECEIVED);
        return requisitionRepository.save(requisition);
    }

    @Transactional
    public Requisition complete(Requisition requisition, AppUser actor) {
        if (requisition.getStatus() != RequisitionStatus.RECEIVED) {
            throw new InvalidStateTransitionException(
                    "Cannot complete requisition in status: " + requisition.getStatus() +
                    ". Expected status: RECEIVED."
            );
        }

        RoleName role = actor.getRole().getName();
        if (role != RoleName.PROCUREMENT_OFFICER && role != RoleName.STORE_KEEPER && role != RoleName.ADMIN) {
            throw new AccessDeniedException("Only PROCUREMENT_OFFICER, STORE_KEEPER, or ADMIN can complete a requisition");
        }

        requisition.setStatus(RequisitionStatus.COMPLETED);
        return requisitionRepository.save(requisition);
    }

    private void recordApproval(Requisition req, String stage, ApprovalDecision decision, AppUser actor, String comment) {
        Approval approval = new Approval(req, stage, decision, actor, comment);
        approvalRepository.save(approval);
    }
}
