package com.linksphere.controller;

import com.linksphere.dto.analytics.AnalyticsSummaryResponse;
import com.linksphere.security.CustomUserDetails;
import com.linksphere.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/urls/{id}/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping
    public ResponseEntity<AnalyticsSummaryResponse> getUrlAnalytics(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long id) {
        AnalyticsSummaryResponse analytics = analyticsService.getUrlAnalytics(userDetails.getId(), id);
        return ResponseEntity.ok(analytics);
    }
}
