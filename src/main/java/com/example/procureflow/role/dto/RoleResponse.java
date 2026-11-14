package com.example.procureflow.role.dto;

import com.example.procureflow.role.entity.Role;

public record RoleResponse(
        Long id,
        String name
) {
    public static RoleResponse fromEntity(Role role) {
        return new RoleResponse(role.getId(), role.getName().name());
    }
}
