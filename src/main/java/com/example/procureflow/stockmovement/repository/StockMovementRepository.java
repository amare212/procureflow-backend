package com.example.procureflow.stockmovement.repository;

import com.example.procureflow.stockmovement.entity.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long>, JpaSpecificationExecutor<StockMovement> {
    List<StockMovement> findByItemIdOrderByCreatedAtDesc(Long itemId);
}
