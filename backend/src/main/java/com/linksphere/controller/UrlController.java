package com.linksphere.controller;

import com.linksphere.dto.url.CreateUrlRequest;
import com.linksphere.dto.url.UpdateUrlRequest;
import com.linksphere.dto.url.UrlResponse;
import com.linksphere.security.CustomUserDetails;
import com.linksphere.service.UrlService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/urls")
@RequiredArgsConstructor
public class UrlController {

    private final UrlService urlService;
    private final com.linksphere.service.RateLimiterService rateLimiterService;

    @PostMapping
    public ResponseEntity<UrlResponse> createShortUrl(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateUrlRequest request) {
        String rateLimitKey = "create_url:user:" + userDetails.getId();
        if (!rateLimiterService.isAllowed(rateLimitKey, 30, java.time.Duration.ofMinutes(1))) {
            throw new com.linksphere.exception.RateLimitExceededException("Rate limit exceeded. Maximum 30 link creations per minute.");
        }
        UrlResponse response = urlService.createShortUrl(userDetails.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<UrlResponse>> getUserUrls(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<UrlResponse> response = urlService.getUserUrls(userDetails.getId(), search, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UrlResponse> getUrlDetails(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        UrlResponse response = urlService.getUrlDetails(userDetails.getId(), id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UrlResponse> updateUrl(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id,
            @Valid @RequestBody UpdateUrlRequest request) {
        UrlResponse response = urlService.updateUrl(userDetails.getId(), id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUrl(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        urlService.deleteUrl(userDetails.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
