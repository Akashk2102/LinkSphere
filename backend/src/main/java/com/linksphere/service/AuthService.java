package com.linksphere.service;

import com.linksphere.dto.auth.AuthResponse;
import com.linksphere.dto.auth.LoginRequest;
import com.linksphere.dto.auth.RegisterRequest;
import com.linksphere.dto.auth.UserSummaryResponse;
import com.linksphere.entity.Role;
import com.linksphere.entity.User;
import com.linksphere.exception.DuplicateResourceException;
import com.linksphere.repository.UserRepository;
import com.linksphere.security.CustomUserDetails;
import com.linksphere.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("An account with email " + email + " already exists");
        }

        User user = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName().trim())
                .role(Role.ROLE_USER)
                .build();

        User savedUser = userRepository.save(user);
        log.info("Registered new user with id: {}", savedUser.getId());

        CustomUserDetails userDetails = new CustomUserDetails(savedUser);
        String token = tokenProvider.generateToken(userDetails);

        return buildAuthResponse(token, savedUser);
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.getPassword())
        );

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String token = tokenProvider.generateToken(userDetails);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User authenticated but not found in database"));

        log.info("User {} successfully logged in", email);
        return buildAuthResponse(token, user);
    }

    public UserSummaryResponse toUserSummary(User user) {
        return UserSummaryResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .build();
    }

    private AuthResponse buildAuthResponse(String token, User user) {
        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .user(toUserSummary(user))
                .build();
    }
}
