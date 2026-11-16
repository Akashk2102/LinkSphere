package com.linksphere.service;

import com.linksphere.dto.url.CachedUrl;
import com.linksphere.dto.url.CreateUrlRequest;
import com.linksphere.dto.url.UpdateUrlRequest;
import com.linksphere.dto.url.UrlResponse;
import com.linksphere.entity.ShortUrl;
import com.linksphere.entity.User;
import com.linksphere.exception.BadRequestException;
import com.linksphere.exception.ResourceNotFoundException;
import com.linksphere.repository.ShortUrlRepository;
import com.linksphere.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class UrlService {

    private final ShortUrlRepository shortUrlRepository;
    private final UserRepository userRepository;
    private final ShortCodeGenerator codeGenerator;
    private final UrlCacheService cacheService;

    @Value("${app.shortener.base-url:http://localhost:8080/r}")
    private String baseUrl;

    @Transactional
    public UrlResponse createShortUrl(Long userId, CreateUrlRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Prevent self-referencing links
        if (request.getOriginalUrl().startsWith(baseUrl)) {
            throw new BadRequestException("Cannot create a short link targeting LinkSphere domain");
        }

        if (request.getExpiresAt() != null && request.getExpiresAt().isBefore(Instant.now())) {
            throw new BadRequestException("Expiration date must be in the future");
        }

        String shortCode;
        boolean isCustom = false;

        if (StringUtils.hasText(request.getCustomAlias())) {
            shortCode = codeGenerator.validateAndNormalizeCustomAlias(request.getCustomAlias());
            isCustom = true;
        } else {
            shortCode = codeGenerator.generateUniqueCode();
        }

        String title = StringUtils.hasText(request.getTitle()) ? request.getTitle().trim() : request.getOriginalUrl();

        ShortUrl shortUrl = ShortUrl.builder()
                .user(user)
                .originalUrl(request.getOriginalUrl().trim())
                .shortCode(shortCode)
                .title(title)
                .isCustomAlias(isCustom)
                .isActive(true)
                .expiresAt(request.getExpiresAt())
                .clickCount(0L)
                .build();

        ShortUrl saved = shortUrlRepository.save(shortUrl);
        log.info("Created short URL {} for original: {}", shortCode, saved.getOriginalUrl());

        // Pre-populate cache for immediate fast lookup
        cacheService.put(shortCode, toCachedUrl(saved));

        return toUrlResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<UrlResponse> getUserUrls(Long userId, String search, Pageable pageable) {
        Page<ShortUrl> page;
        if (StringUtils.hasText(search)) {
            page = shortUrlRepository.searchUserUrls(userId, search.trim(), pageable);
        } else {
            page = shortUrlRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        }
        return page.map(this::toUrlResponse);
    }

    @Transactional(readOnly = true)
    public UrlResponse getUrlDetails(Long userId, Long urlId) {
        ShortUrl shortUrl = findUserUrl(userId, urlId);
        return toUrlResponse(shortUrl);
    }

    @Transactional
    public UrlResponse updateUrl(Long userId, Long urlId, UpdateUrlRequest request) {
        ShortUrl shortUrl = findUserUrl(userId, urlId);

        if (StringUtils.hasText(request.getTitle())) {
            shortUrl.setTitle(request.getTitle().trim());
        }

        if (request.getIsActive() != null) {
            shortUrl.setActive(request.getIsActive());
        }

        if (request.getExpiresAt() != null) {
            if (request.getExpiresAt().isBefore(Instant.now())) {
                throw new BadRequestException("Expiration date must be in the future");
            }
            shortUrl.setExpiresAt(request.getExpiresAt());
        }

        ShortUrl updated = shortUrlRepository.save(shortUrl);

        // Evict stale cache so subsequent redirects read new state or warm cache
        cacheService.evict(updated.getShortCode());
        if (updated.isActive() && !updated.isExpired()) {
            cacheService.put(updated.getShortCode(), toCachedUrl(updated));
        }

        log.info("Updated short URL id: {} (code: {})", urlId, updated.getShortCode());
        return toUrlResponse(updated);
    }

    @Transactional
    public void deleteUrl(Long userId, Long urlId) {
        ShortUrl shortUrl = findUserUrl(userId, urlId);
        String code = shortUrl.getShortCode();

        shortUrlRepository.delete(shortUrl);
        cacheService.evict(code);
        log.info("Deleted short URL id: {} (code: {})", urlId, code);
    }

    private ShortUrl findUserUrl(Long userId, Long urlId) {
        ShortUrl shortUrl = shortUrlRepository.findById(urlId)
                .orElseThrow(() -> new ResourceNotFoundException("Short URL not found with id: " + urlId));

        if (!shortUrl.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You do not have permission to access or modify this link");
        }
        return shortUrl;
    }

    public UrlResponse toUrlResponse(ShortUrl shortUrl) {
        return UrlResponse.builder()
                .id(shortUrl.getId())
                .shortCode(shortUrl.getShortCode())
                .shortUrl(baseUrl + "/" + shortUrl.getShortCode())
                .originalUrl(shortUrl.getOriginalUrl())
                .title(shortUrl.getTitle())
                .isCustomAlias(shortUrl.isCustomAlias())
                .isActive(shortUrl.isActive())
                .isExpired(shortUrl.isExpired())
                .clickCount(shortUrl.getClickCount())
                .expiresAt(shortUrl.getExpiresAt())
                .createdAt(shortUrl.getCreatedAt())
                .build();
    }

    public CachedUrl toCachedUrl(ShortUrl shortUrl) {
        return CachedUrl.builder()
                .id(shortUrl.getId())
                .originalUrl(shortUrl.getOriginalUrl())
                .shortCode(shortUrl.getShortCode())
                .isActive(shortUrl.isActive())
                .expiresAt(shortUrl.getExpiresAt())
                .build();
    }
}
