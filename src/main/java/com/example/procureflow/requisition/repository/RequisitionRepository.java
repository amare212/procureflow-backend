package com.example.procureflow.requisition.repository;

import com.example.procureflow.requisition.entity.Requisition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RequisitionRepository extends JpaRepository<Requisition, Long>, JpaSpecificationExecutor<Requisition> {
    Optional<Requisition> findByRequisitionNumber(String requisitionNumber);
    boolean existsByRequisitionNumber(String requisitionNumber);

    @Query(value = "SELECT nextval('requisition_seq')", nativeQuery = true)
    Long getNextSequenceValue();
}
