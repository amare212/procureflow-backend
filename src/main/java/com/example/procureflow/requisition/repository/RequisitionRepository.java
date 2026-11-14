package com.example.procureflow.requisition.repository;

import com.example.procureflow.requisition.entity.Requisition;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RequisitionRepository extends JpaRepository<Requisition, Long> {
}
