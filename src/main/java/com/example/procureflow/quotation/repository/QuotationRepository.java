package com.example.procureflow.quotation.repository;

import com.example.procureflow.quotation.entity.Quotation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuotationRepository extends JpaRepository<Quotation, Long> {
    List<Quotation> findByRequisitionIdOrderByCreatedAtDesc(Long requisitionId);
    Optional<Quotation> findByRequisitionIdAndSelectedTrue(Long requisitionId);
}
