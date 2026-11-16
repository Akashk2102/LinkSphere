package com.linksphere.service;

import com.linksphere.entity.ShortUrl;
import com.linksphere.repository.ShortUrlRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LinkExpirationScheduler {

    private final ShortUrlRepository shortUrlRepository;
    private final UrlCacheService cacheService;

    /**
     * Runs every hour to clean up and deactivate expired links in bulk.
     * Cron: at minute 0 past every hour.
     */
    @Scheduled(cron = "${app.shortener.expiration-cron:0 0 * * * *}")
    @Transactional
    public void deactivateExpiredLinks() {
        Instant now = Instant.now();
        List<ShortUrl> expiredActiveUrls = shortUrlRepository.findByIsActiveTrueAndExpiresAtBefore(now);

        if (expiredActiveUrls.isEmpty()) {
            return;
        }

        log.info("Found {} expired links still marked as active. Deactivating and evicting cache...", expiredActiveUrls.size());

        for (ShortUrl url : expiredActiveUrls) {
            url.setActive(false);
            cacheService.evict(url.getShortCode());
        }

        shortUrlRepository.saveAll(expiredActiveUrls);
        log.info("Successfully deactivated and cleared cache for {} expired links.", expiredActiveUrls.size());
    }
}
