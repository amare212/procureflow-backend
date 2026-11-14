package com.example.procureflow.user.dto;

import com.example.procureflow.user.entity.AppUser;

import java.time.Instant;

public record UserResponse(
        Long id,
        String fullName,
        String email,
        String role,
        Long departmentId,
        String departmentName,
        boolean enabled,
        Instant createdAt
) {
    public static UserResponse fromEntity(AppUser user) {
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole() != null ? user.getRole().getName().name() : null,
                user.getDepartment() != null ? user.getDepartment().getId() : null,
                user.getDepartment() != null ? user.getDepartment().getName() : null,
                user.isEnabled(),
                user.getCreatedAt()
        );
    }
}
