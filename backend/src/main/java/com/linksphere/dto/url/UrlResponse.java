package com.linksphere.dto.url;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UrlResponse {
    private Long id;
    private String shortCode;
    private String shortUrl;
    private String originalUrl;
    private String title;
    private boolean isCustomAlias;
    private boolean isActive;
    private boolean isExpired;
    private long clickCount;
    private Instant expiresAt;
    private Instant createdAt;
}
