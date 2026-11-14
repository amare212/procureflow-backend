package com.example.procureflow.user.service;

import com.example.procureflow.common.dto.PageResponse;
import com.example.procureflow.common.exception.BusinessException;
import com.example.procureflow.common.exception.ResourceNotFoundException;
import com.example.procureflow.department.entity.Department;
import com.example.procureflow.department.repository.DepartmentRepository;
import com.example.procureflow.role.entity.Role;
import com.example.procureflow.role.entity.RoleName;
import com.example.procureflow.role.repository.RoleRepository;
import com.example.procureflow.user.dto.UserRequest;
import com.example.procureflow.user.dto.UserResponse;
import com.example.procureflow.user.entity.AppUser;
import com.example.procureflow.user.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            DepartmentRepository departmentRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> findAll(String search, RoleName role, Long departmentId, Pageable pageable) {
        Specification<AppUser> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (search != null && !search.trim().isEmpty()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("fullName")), pattern),
                        cb.like(cb.lower(root.get("email")), pattern)
                ));
            }
            if (role != null) {
                predicates.add(cb.equal(root.get("role").get("name"), role));
            }
            if (departmentId != null) {
                predicates.add(cb.equal(root.get("department").get("id"), departmentId));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<UserResponse> page = userRepository.findAll(spec, pageable).map(UserResponse::fromEntity);
        return PageResponse.of(page);
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return UserResponse.fromEntity(user);
    }

    @Transactional
    public UserResponse create(UserRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new BusinessException("User with email '" + request.email() + "' already exists");
        }
        if (request.password() == null || request.password().isBlank()) {
            throw new BusinessException("Password is required for user creation");
        }

        Role role = roleRepository.findByName(request.role())
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + request.role()));

        Department department = null;
        if (request.departmentId() != null) {
            department = departmentRepository.findById(request.departmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + request.departmentId()));
        }

        AppUser user = new AppUser();
        user.setFullName(request.fullName().trim());
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(role);
        user.setDepartment(department);
        user.setEnabled(request.enabled() != null ? request.enabled() : true);

        return UserResponse.fromEntity(userRepository.save(user));
    }

    @Transactional
    public UserResponse update(Long id, UserRequest request) {
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        String normalizedEmail = request.email().trim().toLowerCase();
        if (!user.getEmail().equalsIgnoreCase(normalizedEmail) && userRepository.existsByEmail(normalizedEmail)) {
            throw new BusinessException("User with email '" + request.email() + "' already exists");
        }

        Role role = roleRepository.findByName(request.role())
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + request.role()));

        Department department = null;
        if (request.departmentId() != null) {
            department = departmentRepository.findById(request.departmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found with id: " + request.departmentId()));
        }

        user.setFullName(request.fullName().trim());
        user.setEmail(normalizedEmail);
        user.setRole(role);
        user.setDepartment(department);
        if (request.enabled() != null) {
            user.setEnabled(request.enabled());
        }
        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }

        return UserResponse.fromEntity(userRepository.save(user));
    }

    @Transactional
    public void delete(Long id) {
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        user.setEnabled(false);
        userRepository.save(user);
    }
}
