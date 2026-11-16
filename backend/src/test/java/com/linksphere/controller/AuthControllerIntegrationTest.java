package com.linksphere.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linksphere.dto.auth.AuthResponse;
import com.linksphere.dto.auth.LoginRequest;
import com.linksphere.dto.auth.RegisterRequest;
import com.linksphere.dto.auth.UserSummaryResponse;
import com.linksphere.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @Test
    @DisplayName("POST /api/v1/auth/register should return 201 Created on valid input")
    void shouldRegisterUser() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("jane.dev@example.com")
                .password("StrongPassword123!")
                .fullName("Jane Developer")
                .build();

        AuthResponse mockResponse = AuthResponse.builder()
                .token("jwt-test-token-123")
                .tokenType("Bearer")
                .user(UserSummaryResponse.builder()
                        .id(1L)
                        .email("jane.dev@example.com")
                        .fullName("Jane Developer")
                        .role("ROLE_USER")
                        .build())
                .build();

        when(authService.register(any(RegisterRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt-test-token-123"))
                .andExpect(jsonPath("$.user.email").value("jane.dev@example.com"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/register should return 400 Bad Request on invalid email")
    void shouldFailValidationOnInvalidEmail() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("not-an-email")
                .password("ValidPass123!")
                .fullName("Test User")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.email").exists());
    }

    @Test
    @DisplayName("POST /api/v1/auth/login should return 200 OK on valid credentials")
    void shouldLoginUser() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("jane.dev@example.com")
                .password("StrongPassword123!")
                .build();

        AuthResponse mockResponse = AuthResponse.builder()
                .token("jwt-login-token-456")
                .tokenType("Bearer")
                .user(UserSummaryResponse.builder()
                        .id(1L)
                        .email("jane.dev@example.com")
                        .fullName("Jane Developer")
                        .role("ROLE_USER")
                        .build())
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-login-token-456"));
    }
}
