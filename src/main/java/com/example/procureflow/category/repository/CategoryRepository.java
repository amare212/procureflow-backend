package com.example.procureflow.category.repository;

import com.example.procureflow.category.entity.ItemCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<ItemCategory, Long>, JpaSpecificationExecutor<ItemCategory> {
    Optional<ItemCategory> findByName(String name);
    boolean existsByName(String name);
}
