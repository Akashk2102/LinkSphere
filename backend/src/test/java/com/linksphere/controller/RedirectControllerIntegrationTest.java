package com.linksphere.controller;

import com.linksphere.dto.url.CachedUrl;
import com.linksphere.repository.ShortUrlRepository;
import com.linksphere.service.AnalyticsService;
import com.linksphere.service.RateLimiterService;
import com.linksphere.service.UrlCacheService;
import com.linksphere.service.UrlService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RedirectControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UrlCacheService cacheService;

    @MockBean
    private ShortUrlRepository shortUrlRepository;

    @MockBean
    private UrlService urlService;

    @MockBean
    private AnalyticsService analyticsService;

    @MockBean
    private RateLimiterService rateLimiterService;

    @Test
    @DisplayName("Should return 302 Found with Location header on cache hit")
    void shouldRedirectOnCacheHit() throws Exception {
        CachedUrl cached = CachedUrl.builder()
                .id(1L)
                .shortCode("abc1234")
                .originalUrl("https://kubernetes.io/docs/home/")
                .isActive(true)
                .build();

        when(rateLimiterService.isAllowed(anyString(), anyInt(), any())).thenReturn(true);
        when(cacheService.get("abc1234")).thenReturn(Optional.of(cached));

        mockMvc.perform(get("/r/abc1234")
                        .header("User-Agent", "Mozilla/5.0")
                        .header("Referer", "https://google.com"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://kubernetes.io/docs/home/"));

        verify(analyticsService).recordClickAsync(argThat(event ->
                event.getShortUrlId().equals(1L) &&
                event.getReferrer().equals("https://google.com")
        ));
    }

    @Test
    @DisplayName("Should return 410 Gone when short URL is expired")
    void shouldReturn410WhenExpired() throws Exception {
        CachedUrl expired = CachedUrl.builder()
                .id(2L)
                .shortCode("exp1234")
                .originalUrl("https://example.com")
                .isActive(true)
                .expiresAt(Instant.now().minus(1, ChronoUnit.HOURS))
                .build();

        when(rateLimiterService.isAllowed(anyString(), anyInt(), any())).thenReturn(true);
        when(cacheService.get("exp1234")).thenReturn(Optional.of(expired));

        mockMvc.perform(get("/r/exp1234"))
                .andExpect(status().isGone());
    }

    @Test
    @DisplayName("Should return 404 Not Found when short code does not exist anywhere")
    void shouldReturn404WhenNotFound() throws Exception {
        when(rateLimiterService.isAllowed(anyString(), anyInt(), any())).thenReturn(true);
        when(cacheService.get("missing")).thenReturn(Optional.empty());
        when(shortUrlRepository.findByShortCode("missing")).thenReturn(Optional.empty());

        mockMvc.perform(get("/r/missing"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 429 Too Many Requests when rate limit exceeded")
    void shouldReturn429WhenRateLimited() throws Exception {
        when(rateLimiterService.isAllowed(anyString(), anyInt(), any())).thenReturn(false);

        mockMvc.perform(get("/r/spamming"))
                .andExpect(status().isTooManyRequests());
    }
}
