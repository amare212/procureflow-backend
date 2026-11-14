package com.example.procureflow.item.service;

import com.example.procureflow.category.entity.ItemCategory;
import com.example.procureflow.category.repository.CategoryRepository;
import com.example.procureflow.common.dto.PageResponse;
import com.example.procureflow.common.exception.BusinessException;
import com.example.procureflow.common.exception.ResourceNotFoundException;
import com.example.procureflow.item.dto.ItemRequest;
import com.example.procureflow.item.dto.ItemResponse;
import com.example.procureflow.item.entity.Item;
import com.example.procureflow.item.repository.ItemRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class ItemService {

    private final ItemRepository itemRepository;
    private final CategoryRepository categoryRepository;

    public ItemService(ItemRepository itemRepository, CategoryRepository categoryRepository) {
        this.itemRepository = itemRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<ItemResponse> findAll(String search, Long categoryId, Boolean active, Pageable pageable) {
        Specification<Item> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (search != null && !search.trim().isEmpty()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("sku")), pattern)
                ));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<ItemResponse> page = itemRepository.findAll(spec, pageable).map(ItemResponse::fromEntity);
        return PageResponse.of(page);
    }

    @Transactional(readOnly = true)
    public ItemResponse findById(Long id) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + id));
        return ItemResponse.fromEntity(item);
    }

    @Transactional
    public ItemResponse create(ItemRequest request) {
        if (itemRepository.existsBySku(request.sku().trim().toUpperCase())) {
            throw new BusinessException("Item with SKU '" + request.sku() + "' already exists");
        }

        ItemCategory category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.categoryId()));

        Item item = new Item();
        item.setSku(request.sku().trim().toUpperCase());
        item.setName(request.name().trim());
        item.setDescription(request.description());
        item.setCategory(category);
        item.setUnit(request.unit().trim());
        item.setUnitPrice(request.unitPrice());
        item.setReorderLevel(request.reorderLevel() != null ? request.reorderLevel() : 0);
        item.setActive(request.active() != null ? request.active() : true);

        return ItemResponse.fromEntity(itemRepository.save(item));
    }

    @Transactional
    public ItemResponse update(Long id, ItemRequest request) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Item not found with id: " + id));

        if (!item.getSku().equalsIgnoreCase(request.sku().trim()) && itemRepository.existsBySku(request.sku().trim().toUpperCase())) {
            throw new BusinessException("Item with SKU '" + request.sku() + "' already exists");
        }

        ItemCategory category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.categoryId()));

        item.setSku(request.sku().trim().toUpperCase());
        item.setName(request.name().trim());
        item.setDescription(request.description());
        item.setCategory(category);
        item.setUnit(request.unit().trim());
        item.setUnitPrice(request.unitPrice());
        if (request.reorderLevel() != null) {
            item.setReorderLevel(request.reorderLevel());
        }
        if (request.active() != null) {
            item.setActive(request.active());
        }

        return ItemResponse.fromEntity(itemRepository.save(item));
    }

    @Transactional
    public void delete(Long id) {
        if (!itemRepository.existsById(id)) {
            throw new ResourceNotFoundException("Item not found with id: " + id);
        }
        itemRepository.deleteById(id);
    }
}
