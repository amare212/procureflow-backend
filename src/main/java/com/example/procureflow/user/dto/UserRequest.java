package com.example.procureflow.user.dto;

import com.example.procureflow.role.entity.RoleName;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserRequest(
        @NotBlank(message = "Full name is required")
        @Size(max = 150, message = "Name must not exceed 150 characters")
        String fullName,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        @Size(max = 180, message = "Email must not exceed 180 characters")
        String email,

        @Size(min = 6, message = "Password must be at least 6 characters")
        String password,

        Long departmentId,

        @NotNull(message = "Role is required")
        RoleName role,

        Boolean enabled
) {
}
