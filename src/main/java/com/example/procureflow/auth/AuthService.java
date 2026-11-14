package com.example.procureflow.auth;

import com.example.procureflow.auth.dto.LoginRequest;
import com.example.procureflow.auth.dto.LoginResponse;
import com.example.procureflow.security.CurrentUser;
import com.example.procureflow.security.JwtService;
import com.example.procureflow.user.dto.UserResponse;
import com.example.procureflow.user.entity.AppUser;
import com.example.procureflow.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final CurrentUser currentUser;
    private final long expirationMs;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            UserDetailsService userDetailsService,
            CurrentUser currentUser,
            @Value("${app.jwt.expiration-ms:3600000}") long expirationMs
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.currentUser = currentUser;
        this.expirationMs = expirationMs;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        AppUser user = userRepository.findByEmail(request.email().trim().toLowerCase())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!user.isEnabled()) {
            throw new BadCredentialsException("User account is disabled");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String roleName = user.getRole().getName().name();
        Long departmentId = user.getDepartment() != null ? user.getDepartment().getId() : null;

        String token = jwtService.generateToken(userDetails, user.getId(), roleName, departmentId);

        return LoginResponse.of(token, expirationMs, UserResponse.fromEntity(user));
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser() {
        AppUser user = currentUser.getUser();
        return UserResponse.fromEntity(user);
    }
}
