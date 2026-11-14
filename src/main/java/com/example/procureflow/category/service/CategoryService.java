package com.example.procureflow.category.service;

import com.example.procureflow.category.dto.CategoryRequest;
import com.example.procureflow.category.dto.CategoryResponse;
import com.example.procureflow.category.entity.ItemCategory;
import com.example.procureflow.category.repository.CategoryRepository;
import com.example.procureflow.common.dto.PageResponse;
import com.example.procureflow.common.exception.BusinessException;
import com.example.procureflow.common.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<CategoryResponse> findAll(String search, Pageable pageable) {
        Specification<ItemCategory> spec = (root, query, cb) -> {
            if (search == null || search.trim().isEmpty()) {
                return cb.conjunction();
            }
            String pattern = "%" + search.trim().toLowerCase() + "%";
            return cb.like(cb.lower(root.get("name")), pattern);
        };

        Page<CategoryResponse> page = categoryRepository.findAll(spec, pageable)
                .map(CategoryResponse::fromEntity);
        return PageResponse.of(page);
    }

    @Transactional(readOnly = true)
    public CategoryResponse findById(Long id) {
        ItemCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        return CategoryResponse.fromEntity(category);
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        if (categoryRepository.existsByName(request.name().trim())) {
            throw new BusinessException("Category with name '" + request.name() + "' already exists");
        }

        ItemCategory category = new ItemCategory(request.name().trim(), request.description());
        return CategoryResponse.fromEntity(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        ItemCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        if (!category.getName().equalsIgnoreCase(request.name().trim()) && categoryRepository.existsByName(request.name().trim())) {
            throw new BusinessException("Category with name '" + request.name() + "' already exists");
        }

        category.setName(request.name().trim());
        category.setDescription(request.description());

        return CategoryResponse.fromEntity(categoryRepository.save(category));
    }

    @Transactional
    public void delete(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Category not found with id: " + id);
        }
        categoryRepository.deleteById(id);
    }
}
