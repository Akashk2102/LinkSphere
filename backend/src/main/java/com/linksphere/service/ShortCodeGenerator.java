package com.linksphere.service;

import com.linksphere.exception.BadRequestException;
import com.linksphere.exception.DuplicateResourceException;
import com.linksphere.repository.ShortUrlRepository;
import com.linksphere.util.Base62;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShortCodeGenerator {

    private static final int DEFAULT_CODE_LENGTH = 7;
    private static final int MAX_COLLISION_RETRIES = 5;
    private static final Pattern ALIAS_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{3,32}$");

    private static final Set<String> RESERVED_KEYWORDS = Set.of(
            "api", "auth", "r", "admin", "login", "register", "dashboard",
            "health", "metrics", "actuator", "static", "assets", "favicon.ico",
            "settings", "profile", "links", "analytics"
    );

    private final ShortUrlRepository shortUrlRepository;

    /**
     * Generates a unique 7-character short code, handling collision retries safely.
     */
    public String generateUniqueCode() {
        for (int attempt = 1; attempt <= MAX_COLLISION_RETRIES; attempt++) {
            String candidate = Base62.generateRandom(DEFAULT_CODE_LENGTH);

            if (!RESERVED_KEYWORDS.contains(candidate.toLowerCase()) && !shortUrlRepository.existsByShortCode(candidate)) {
                return candidate;
            }
            log.warn("Collision detected for candidate short code: {}. Retrying attempt {}/{}", candidate, attempt, MAX_COLLISION_RETRIES);
        }

        // Fallback: increase length by 1 if repeated collisions occur
        return Base62.generateRandom(DEFAULT_CODE_LENGTH + 1);
    }

    /**
     * Validates and verifies uniqueness for user-provided custom alias.
     */
    public String validateAndNormalizeCustomAlias(String customAlias) {
        if (customAlias == null || customAlias.isBlank()) {
            throw new BadRequestException("Custom alias cannot be blank");
        }

        String normalized = customAlias.trim();

        if (!ALIAS_PATTERN.matcher(normalized).matches()) {
            throw new BadRequestException("Custom alias must be between 3 and 32 characters and only contain alphanumeric characters, hyphens, and underscores.");
        }

        if (RESERVED_KEYWORDS.contains(normalized.toLowerCase())) {
            throw new BadRequestException("The alias '" + normalized + "' is a reserved keyword and cannot be used.");
        }

        if (shortUrlRepository.existsByShortCode(normalized)) {
            throw new DuplicateResourceException("The alias '" + normalized + "' is already in use. Please choose another.");
        }

        return normalized;
    }
}
