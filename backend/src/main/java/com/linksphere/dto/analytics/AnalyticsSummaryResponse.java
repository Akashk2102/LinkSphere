package com.linksphere.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalyticsSummaryResponse {
    private Long shortUrlId;
    private String shortCode;
    private String originalUrl;
    private long totalClicks;
    private List<DistributionItem> devices;
    private List<DistributionItem> browsers;
    private List<DistributionItem> referrers;
    private List<ClickTimeSeriesPoint> timeline;
}
