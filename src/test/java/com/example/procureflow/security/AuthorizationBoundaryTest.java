package com.example.procureflow.security;

import com.example.procureflow.role.entity.Role;
import com.example.procureflow.role.entity.RoleName;
import com.example.procureflow.user.entity.AppUser;
import com.example.procureflow.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthorizationBoundaryTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    private AppUser admin;
    private AppUser employee;

    @BeforeEach
    void setUp() {
        Role adminRole = new Role(RoleName.ADMIN);
        admin = new AppUser();
        admin.setEmail("admin@procureflow.com");
        admin.setPasswordHash("hashed_pwd");
        admin.setRole(adminRole);
        admin.setEnabled(true);

        Role empRole = new Role(RoleName.EMPLOYEE);
        employee = new AppUser();
        employee.setEmail("emp@procureflow.com");
        employee.setPasswordHash("hashed_pwd");
        employee.setRole(empRole);
        employee.setEnabled(true);
    }

    @Test
    void testAdminGrantedAuthorityHasRolePrefix() {
        when(userRepository.findByEmail("admin@procureflow.com")).thenReturn(Optional.of(admin));

        UserDetails userDetails = userDetailsService.loadUserByUsername("admin@procureflow.com");

        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void testEmployeeGrantedAuthorityHasRolePrefix() {
        when(userRepository.findByEmail("emp@procureflow.com")).thenReturn(Optional.of(employee));

        UserDetails userDetails = userDetailsService.loadUserByUsername("emp@procureflow.com");

        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_EMPLOYEE")));
        assertFalse(userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
    }

    @Test
    void testUnauthenticatedCurrentUserThrowsAccessDenied() {
        assertThrows(AccessDeniedException.class, CurrentUser::getAuthenticatedEmail);
    }
}
