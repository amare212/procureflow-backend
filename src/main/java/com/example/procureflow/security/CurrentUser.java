package com.example.procureflow.security;

import com.example.procureflow.common.exception.ResourceNotFoundException;
import com.example.procureflow.user.entity.AppUser;
import com.example.procureflow.user.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {

    private final UserRepository userRepository;

    public CurrentUser(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public static String getAuthenticatedEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AccessDeniedException("User is not authenticated");
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof UserDetails ud) {
            return ud.getUsername();
        } else if (principal instanceof String str) {
            return str;
        }
        throw new AccessDeniedException("Unable to determine authenticated user");
    }

    public AppUser getUser() {
        String email = getAuthenticatedEmail();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found with email: " + email));
    }

    public Long getUserId() {
        return getUser().getId();
    }

    public String getRole() {
        return getUser().getRole().getName().name();
    }

    public Long getDepartmentId() {
        return getUser().getDepartment() != null ? getUser().getDepartment().getId() : null;
    }
}
