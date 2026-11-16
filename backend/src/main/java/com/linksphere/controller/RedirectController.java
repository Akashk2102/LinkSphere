package com.linksphere.controller;

import com.linksphere.dto.analytics.ClickEvent;
import com.linksphere.dto.url.CachedUrl;
import com.linksphere.entity.ShortUrl;
import com.linksphere.exception.ResourceNotFoundException;
import com.linksphere.repository.ShortUrlRepository;
import com.linksphere.service.AnalyticsService;
import com.linksphere.service.UrlCacheService;
import com.linksphere.service.UrlService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Optional;

@RestController
@RequestMapping("/r")
@RequiredArgsConstructor
@Slf4j
public class RedirectController {

    private final UrlCacheService cacheService;
    private final ShortUrlRepository shortUrlRepository;
    private final UrlService urlService;
    private final AnalyticsService analyticsService;
    private final com.linksphere.service.RateLimiterService rateLimiterService;

    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(
            @PathVariable String shortCode,
            HttpServletRequest request) {

        String ip = extractClientIp(request);
        String rateLimitKey = "redirect:ip:" + ip;
        if (!rateLimiterService.isAllowed(rateLimitKey, 120, java.time.Duration.ofMinutes(1))) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }

        // 1. Fast Cache Lookup (sub-millisecond)
        Optional<CachedUrl> cached = cacheService.get(shortCode);
        CachedUrl target;

        if (cached.isPresent()) {
            target = cached.get();
        } else {
            // 2. Cache Miss: Fall back to PostgreSQL
            ShortUrl dbUrl = shortUrlRepository.findByShortCode(shortCode)
                    .orElseThrow(() -> new ResourceNotFoundException("Short URL '" + shortCode + "' does not exist."));

            target = urlService.toCachedUrl(dbUrl);
            // Populate Redis cache for future reads
            cacheService.put(shortCode, target);
        }

        // 3. Validation: Active & Expiration checks
        if (!target.isActive() || target.isExpired()) {
            log.info("Redirect requested for disabled or expired link: {}", shortCode);
            return ResponseEntity.status(HttpStatus.GONE).build();
        }

        // 4. Fire Non-Blocking Asynchronous Click Analytics
        String referrer = request.getHeader("Referer");
        String userAgent = request.getHeader("User-Agent");
        String ip = extractClientIp(request);

        ClickEvent clickEvent = ClickEvent.builder()
                .shortUrlId(target.getId())
                .referrer(referrer)
                .userAgent(userAgent)
                .ipAddress(ip)
                .build();

        analyticsService.recordClickAsync(clickEvent);

        // 5. Immediate HTTP 302 Found Redirect
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(target.getOriginalUrl()))
                .build();
    }

    private String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
