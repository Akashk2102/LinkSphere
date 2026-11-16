package com.linksphere.controller;

import com.linksphere.dto.auth.AuthResponse;
import com.linksphere.dto.auth.LoginRequest;
import com.linksphere.dto.auth.RegisterRequest;
import com.linksphere.dto.auth.UserSummaryResponse;
import com.linksphere.security.CustomUserDetails;
import com.linksphere.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserSummaryResponse> getCurrentUser(@AuthenticationPrincipal CustomUserDetails userDetails) {
        UserSummaryResponse response = UserSummaryResponse.builder()
                .id(userDetails.getId())
                .email(userDetails.getEmail())
                .fullName(userDetails.getFullName())
                .role(userDetails.getAuthorities().iterator().next().getAuthority())
                .build();
        return ResponseEntity.ok(response);
    }
}
