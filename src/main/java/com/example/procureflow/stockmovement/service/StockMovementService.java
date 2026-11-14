package com.example.procureflow.stockmovement.service;

import com.example.procureflow.common.dto.PageResponse;
import com.example.procureflow.stockmovement.dto.StockMovementResponse;
import com.example.procureflow.stockmovement.entity.MovementType;
import com.example.procureflow.stockmovement.entity.StockMovement;
import com.example.procureflow.stockmovement.repository.StockMovementRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class StockMovementService {

    private final StockMovementRepository stockMovementRepository;

    public StockMovementService(StockMovementRepository stockMovementRepository) {
        this.stockMovementRepository = stockMovementRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<StockMovementResponse> findAll(Long itemId, MovementType movementType, Pageable pageable) {
        Specification<StockMovement> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (itemId != null) {
                predicates.add(cb.equal(root.get("item").get("id"), itemId));
            }
            if (movementType != null) {
                predicates.add(cb.equal(root.get("movementType"), movementType));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<StockMovementResponse> page = stockMovementRepository.findAll(spec, pageable)
                .map(StockMovementResponse::fromEntity);
        return PageResponse.of(page);
    }
}
