package com.example.procureflow.department.service;

import com.example.procureflow.common.dto.PageResponse;
import com.example.procureflow.common.exception.BusinessException;
import com.example.procureflow.common.exception.ResourceNotFoundException;
import com.example.procureflow.department.dto.DepartmentRequest;
import com.example.procureflow.department.dto.DepartmentResponse;
import com.example.procureflow.department.entity.Department;
import com.example.procureflow.department.repository.DepartmentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public DepartmentService(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<DepartmentResponse> findAll(String search, Pageable pageable) {
        Specification<Department> spec = (root, query, cb) -> {
            if (search == null || search.trim().isEmpty()) {
                return cb.conjunction();
            }
            String pattern = "%" + search.trim().toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("name")), pattern),
                    cb.like(cb.lower(root.get("code")), pattern)
            );
        };

        Page<DepartmentResponse> page = departmentRepository.findAll(spec, pageable)
                .map(DepartmentResponse::fromEntity);
        return PageResponse.of(page);
    }

    @Transactional(readOnly = true)
    public DepartmentResponse findById(Long id) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));
        return DepartmentResponse.fromEntity(dept);
    }

    @Transactional
    public DepartmentResponse create(DepartmentRequest request) {
        if (departmentRepository.existsByName(request.name().trim())) {
            throw new BusinessException("Department with name '" + request.name() + "' already exists");
        }
        if (request.code() != null && !request.code().trim().isEmpty() && departmentRepository.existsByCode(request.code().trim())) {
            throw new BusinessException("Department with code '" + request.code() + "' already exists");
        }

        Department dept = new Department(
                request.name().trim(),
                request.code() != null ? request.code().trim().toUpperCase() : null,
                request.budget()
        );
        Department saved = departmentRepository.save(dept);
        return DepartmentResponse.fromEntity(saved);
    }

    @Transactional
    public DepartmentResponse update(Long id, DepartmentRequest request) {
        Department dept = departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + id));

        if (!dept.getName().equalsIgnoreCase(request.name().trim()) && departmentRepository.existsByName(request.name().trim())) {
            throw new BusinessException("Department with name '" + request.name() + "' already exists");
        }

        dept.setName(request.name().trim());
        if (request.code() != null) {
            dept.setCode(request.code().trim().toUpperCase());
        }
        if (request.budget() != null) {
            dept.setBudget(request.budget());
        }

        return DepartmentResponse.fromEntity(departmentRepository.save(dept));
    }

    @Transactional
    public void delete(Long id) {
        if (!departmentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Department not found with id: " + id);
        }
        departmentRepository.deleteById(id);
    }
}
