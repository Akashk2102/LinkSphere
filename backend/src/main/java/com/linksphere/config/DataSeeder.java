package com.linksphere.config;

import com.linksphere.dto.auth.RegisterRequest;
import com.linksphere.dto.url.CreateUrlRequest;
import com.linksphere.dto.url.UrlResponse;
import com.linksphere.entity.ClickAnalytics;
import com.linksphere.entity.Role;
import com.linksphere.entity.ShortUrl;
import com.linksphere.entity.User;
import com.linksphere.repository.ClickAnalyticsRepository;
import com.linksphere.repository.ShortUrlRepository;
import com.linksphere.repository.UserRepository;
import com.linksphere.service.AuthService;
import com.linksphere.service.UrlService;
import com.linksphere.util.IpHasher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Random;

@Component
@Profile({"dev", "default"})
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ShortUrlRepository shortUrlRepository;
    private final ClickAnalyticsRepository analyticsRepository;
    private final AuthService authService;
    private final UrlService urlService;

    @Override
    public void run(String... args) {
        if (userRepository.existsByEmail("demo@linksphere.dev")) {
            log.info("Demo data already seeded. Skipping seeder.");
            return;
        }

        log.info("Seeding initial demo data for LinkSphere...");

        // 1. Create Demo User
        RegisterRequest registerReq = RegisterRequest.builder()
                .email("demo@linksphere.dev")
                .password("Demo12345!")
                .fullName("Alex Rivera")
                .build();
        authService.register(registerReq);

        User demoUser = userRepository.findByEmail("demo@linksphere.dev")
                .orElseThrow();

        // 2. Create Sample Short Links
        UrlResponse url1 = urlService.createShortUrl(demoUser.getId(), CreateUrlRequest.builder()
                .originalUrl("https://spring.io/projects/spring-boot")
                .title("Spring Boot Official Documentation")
                .customAlias("spring-docs")
                .build());

        UrlResponse url2 = urlService.createShortUrl(demoUser.getId(), CreateUrlRequest.builder()
                .originalUrl("https://github.com/kubernetes/kubernetes")
                .title("Kubernetes Source Repository")
                .customAlias("k8s-repo")
                .build());

        UrlResponse url3 = urlService.createShortUrl(demoUser.getId(), CreateUrlRequest.builder()
                .originalUrl("https://redis.io/docs/latest/develop/data-types/")
                .title("Redis Data Structures & Caching Patterns")
                .build());

        // 3. Seed Realistic Click Stream Data for URL 1
        ShortUrl shortUrl1 = shortUrlRepository.findById(url1.getId()).orElseThrow();
        seedClicks(shortUrl1, 185);

        // Seed Clicks for URL 2
        ShortUrl shortUrl2 = shortUrlRepository.findById(url2.getId()).orElseThrow();
        seedClicks(shortUrl2, 74);

        log.info("Demo data seeded successfully! Log in with demo@linksphere.dev / Demo12345!");
    }

    private void seedClicks(ShortUrl shortUrl, int count) {
        String[] devices = {"Desktop", "Mobile", "Mobile", "Desktop", "Tablet"};
        String[] browsers = {"Chrome", "Chrome", "Safari", "Firefox", "Edge"};
        String[] oss = {"Windows", "macOS", "iOS", "Android", "Linux"};
        String[] referrers = {"Direct", "https://twitter.com/", "https://linkedin.com/", "https://github.com/", "https://google.com/"};

        Random rand = new Random(42);
        Instant now = Instant.now();

        for (int i = 0; i < count; i++) {
            // Distribute across past 25 days
            int daysAgo = rand.nextInt(25);
            int hoursAgo = rand.nextInt(24);
            Instant clickTime = now.minus(daysAgo, ChronoUnit.DAYS).minus(hoursAgo, ChronoUnit.HOURS);

            ClickAnalytics click = ClickAnalytics.builder()
                    .shortUrl(shortUrl)
                    .clickedAt(clickTime)
                    .deviceType(devices[rand.nextInt(devices.length)])
                    .browser(browsers[rand.nextInt(browsers.length)])
                    .operatingSystem(oss[rand.nextInt(oss.length)])
                    .referrer(referrers[rand.nextInt(referrers.length)])
                    .ipHash(IpHasher.hashIp("192.168.1." + rand.nextInt(254)))
                    .build();

            analyticsRepository.save(click);
            shortUrlRepository.incrementClickCount(shortUrl.getId());
        }
    }
}
