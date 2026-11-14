package com.example.procureflow.category.repository;

import com.example.procureflow.category.entity.ItemCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<ItemCategory, Long> {
}
