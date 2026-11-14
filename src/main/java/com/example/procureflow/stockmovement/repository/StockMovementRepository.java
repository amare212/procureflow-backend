package com.example.procureflow.stockmovement.repository;

import com.example.procureflow.stockmovement.entity.StockMovement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
}
