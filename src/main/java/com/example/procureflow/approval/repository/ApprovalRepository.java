package com.example.procureflow.approval.repository;

import com.example.procureflow.approval.entity.Approval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApprovalRepository extends JpaRepository<Approval, Long> {
    List<Approval> findByRequisitionIdOrderByCreatedAtAsc(Long requisitionId);
}
