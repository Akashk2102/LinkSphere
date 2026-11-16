package com.linksphere.service;

import com.linksphere.dto.analytics.AnalyticsSummaryResponse;
import com.linksphere.dto.analytics.ClickEvent;
import com.linksphere.dto.analytics.ClickTimeSeriesPoint;
import com.linksphere.dto.analytics.DistributionItem;
import com.linksphere.entity.ClickAnalytics;
import com.linksphere.entity.ShortUrl;
import com.linksphere.exception.ResourceNotFoundException;
import com.linksphere.repository.ClickAnalyticsRepository;
import com.linksphere.repository.ShortUrlRepository;
import com.linksphere.util.IpHasher;
import com.linksphere.util.UserAgentParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsService {

    private final ClickAnalyticsRepository analyticsRepository;
    private final ShortUrlRepository shortUrlRepository;

    @Async("analyticsExecutor")
    @Transactional
    public void recordClickAsync(ClickEvent event) {
        try {
            ShortUrl shortUrl = shortUrlRepository.findById(event.getShortUrlId())
                    .orElse(null);

            if (shortUrl == null) {
                log.warn("Cannot record click for non-existent ShortUrl id: {}", event.getShortUrlId());
                return;
            }

            UserAgentParser.DeviceInfo deviceInfo = UserAgentParser.parse(event.getUserAgent());
            String hashedIp = IpHasher.hashIp(event.getIpAddress());

            ClickAnalytics analytics = ClickAnalytics.builder()
                    .shortUrl(shortUrl)
                    .referrer(event.getReferrer())
                    .deviceType(deviceInfo.getDeviceType())
                    .browser(deviceInfo.getBrowser())
                    .operatingSystem(deviceInfo.getOperatingSystem())
                    .ipHash(hashedIp)
                    .build();

            analyticsRepository.save(analytics);
            shortUrlRepository.incrementClickCount(shortUrl.getId());

            log.debug("Recorded click for url id: {} from device: {}, browser: {}",
                    shortUrl.getId(), deviceInfo.getDeviceType(), deviceInfo.getBrowser());
        } catch (Exception ex) {
            log.error("Failed to record async click for url id: {}", event.getShortUrlId(), ex);
        }
    }

    @Transactional(readOnly = true)
    public AnalyticsSummaryResponse getUrlAnalytics(Long userId, Long urlId) {
        ShortUrl shortUrl = shortUrlRepository.findById(urlId)
                .orElseThrow(() -> new ResourceNotFoundException("Short URL not found with id: " + urlId));

        if (!shortUrl.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("You do not have permission to view analytics for this URL");
        }

        long totalClicks = shortUrl.getClickCount();

        List<DistributionItem> devices = toDistributionList(analyticsRepository.getDeviceStats(urlId), totalClicks);
        List<DistributionItem> browsers = toDistributionList(analyticsRepository.getBrowserStats(urlId), totalClicks);
        List<DistributionItem> referrers = toDistributionList(analyticsRepository.getReferrerStats(urlId), totalClicks);

        // Timeline: last 30 days
        Instant thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS);
        List<ClickAnalytics> recentClicks = analyticsRepository.findRecentClicks(urlId, thirtyDaysAgo);

        Map<LocalDate, Long> clicksByDate = recentClicks.stream()
                .collect(Collectors.groupingBy(
                        c -> c.getClickedAt().atZone(ZoneOffset.UTC).toLocalDate(),
                        Collectors.counting()
                ));

        List<ClickTimeSeriesPoint> timeline = new ArrayList<>();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        for (int i = 29; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            long count = clicksByDate.getOrDefault(date, 0L);
            timeline.add(new ClickTimeSeriesPoint(date.toString(), count));
        }

        return AnalyticsSummaryResponse.builder()
                .shortUrlId(shortUrl.getId())
                .shortCode(shortUrl.getShortCode())
                .originalUrl(shortUrl.getOriginalUrl())
                .totalClicks(totalClicks)
                .devices(devices)
                .browsers(browsers)
                .referrers(referrers)
                .timeline(timeline)
                .build();
    }

    private List<DistributionItem> toDistributionList(
            List<ClickAnalyticsRepository.AnalyticsAggregation> rawList, long total) {
        return rawList.stream().map(item -> {
            long count = item.getCount() != null ? item.getCount() : 0L;
            double pct = total > 0 ? Math.round((count * 100.0 / total) * 10.0) / 10.0 : 0.0;
            return new DistributionItem(item.getLabel() != null ? item.getLabel() : "Unknown", count, pct);
        }).toList();
    }
}
