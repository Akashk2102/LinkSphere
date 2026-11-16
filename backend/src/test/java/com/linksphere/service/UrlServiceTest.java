package com.linksphere.service;

import com.linksphere.dto.url.CreateUrlRequest;
import com.linksphere.dto.url.UrlResponse;
import com.linksphere.entity.Role;
import com.linksphere.entity.ShortUrl;
import com.linksphere.entity.User;
import com.linksphere.exception.BadRequestException;
import com.linksphere.repository.ShortUrlRepository;
import com.linksphere.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrlServiceTest {

    @Mock
    private ShortUrlRepository shortUrlRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ShortCodeGenerator codeGenerator;

    @Mock
    private UrlCacheService cacheService;

    @InjectMocks
    private UrlService urlService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(urlService, "baseUrl", "http://localhost:8080/r");
        sampleUser = User.builder()
                .id(1L)
                .email("alex@example.com")
                .fullName("Alex Rivera")
                .role(Role.ROLE_USER)
                .build();
    }

    @Test
    @DisplayName("Should create short URL successfully with generated code")
    void shouldCreateShortUrl() {
        CreateUrlRequest request = CreateUrlRequest.builder()
                .originalUrl("https://spring.io/projects/spring-boot")
                .title("Spring Boot Documentation")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(codeGenerator.generateUniqueCode()).thenReturn("spgDocs");

        ShortUrl mockSaved = ShortUrl.builder()
                .id(10L)
                .user(sampleUser)
                .originalUrl(request.getOriginalUrl())
                .shortCode("spgDocs")
                .title("Spring Boot Documentation")
                .isActive(true)
                .clickCount(0L)
                .createdAt(Instant.now())
                .build();

        when(shortUrlRepository.save(any(ShortUrl.class))).thenReturn(mockSaved);

        UrlResponse response = urlService.createShortUrl(1L, request);

        assertThat(response).isNotNull();
        assertThat(response.getShortCode()).isEqualTo("spgDocs");
        assertThat(response.getShortUrl()).isEqualTo("http://localhost:8080/r/spgDocs");
        assertThat(response.getOriginalUrl()).isEqualTo(request.getOriginalUrl());

        verify(cacheService).put(eq("spgDocs"), any());
    }

    @Test
    @DisplayName("Should reject self-referential LinkSphere URL")
    void shouldRejectSelfReferentialUrl() {
        CreateUrlRequest request = CreateUrlRequest.builder()
                .originalUrl("http://localhost:8080/r/recursive")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        assertThatThrownBy(() -> urlService.createShortUrl(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("targeting LinkSphere domain");
    }

    @Test
    @DisplayName("Should reject expiration date in the past")
    void shouldRejectPastExpirationDate() {
        CreateUrlRequest request = CreateUrlRequest.builder()
                .originalUrl("https://example.com")
                .expiresAt(Instant.now().minus(1, ChronoUnit.DAYS))
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        assertThatThrownBy(() -> urlService.createShortUrl(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("must be in the future");
    }
}
