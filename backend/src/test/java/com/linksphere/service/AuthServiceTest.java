package com.linksphere.service;

import com.linksphere.dto.auth.AuthResponse;
import com.linksphere.dto.auth.LoginRequest;
import com.linksphere.dto.auth.RegisterRequest;
import com.linksphere.entity.Role;
import com.linksphere.entity.User;
import com.linksphere.exception.DuplicateResourceException;
import com.linksphere.repository.UserRepository;
import com.linksphere.security.CustomUserDetails;
import com.linksphere.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider tokenProvider;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("Should successfully register a new user")
    void shouldRegisterNewUser() {
        RegisterRequest request = RegisterRequest.builder()
                .email("newuser@example.com")
                .password("StrongPassword123!")
                .fullName("Taylor Swift")
                .build();

        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(passwordEncoder.encode("StrongPassword123!")).thenReturn("hashedPassword123");

        User savedUser = User.builder()
                .id(5L)
                .email("newuser@example.com")
                .fullName("Taylor Swift")
                .passwordHash("hashedPassword123")
                .role(Role.ROLE_USER)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(tokenProvider.generateToken(any(CustomUserDetails.class))).thenReturn("mock-jwt-token");

        AuthResponse response = authService.register(request);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("mock-jwt-token");
        assertThat(response.getUser().getEmail()).isEqualTo("newuser@example.com");

        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Should reject registration with existing email")
    void shouldRejectDuplicateEmail() {
        RegisterRequest request = RegisterRequest.builder()
                .email("existing@example.com")
                .password("Password123!")
                .fullName("Existing User")
                .build();

        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("Should successfully login user with valid credentials")
    void shouldLoginValidUser() {
        LoginRequest request = LoginRequest.builder()
                .email("user@example.com")
                .password("Password123!")
                .build();

        User user = User.builder()
                .id(2L)
                .email("user@example.com")
                .fullName("Jane Doe")
                .passwordHash("hashed")
                .role(Role.ROLE_USER)
                .build();

        CustomUserDetails userDetails = new CustomUserDetails(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(tokenProvider.generateToken(any(CustomUserDetails.class))).thenReturn("auth-token-xyz");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        AuthResponse response = authService.login(request);

        assertThat(response.getToken()).isEqualTo("auth-token-xyz");
        assertThat(response.getUser().getEmail()).isEqualTo("user@example.com");
    }
}
