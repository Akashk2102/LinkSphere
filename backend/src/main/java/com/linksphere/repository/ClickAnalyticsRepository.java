package com.linksphere.repository;

import com.linksphere.entity.ClickAnalytics;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface ClickAnalyticsRepository extends JpaRepository<ClickAnalytics, Long> {

    long countByShortUrlId(Long shortUrlId);

    @Query("""
        SELECT c.deviceType as label, COUNT(c) as count
        FROM ClickAnalytics c
        WHERE c.shortUrl.id = :shortUrlId
        GROUP BY c.deviceType
        ORDER BY count DESC
    """)
    List<AnalyticsAggregation> getDeviceStats(@Param("shortUrlId") Long shortUrlId);

    @Query("""
        SELECT c.browser as label, COUNT(c) as count
        FROM ClickAnalytics c
        WHERE c.shortUrl.id = :shortUrlId
        GROUP BY c.browser
        ORDER BY count DESC
    """)
    List<AnalyticsAggregation> getBrowserStats(@Param("shortUrlId") Long shortUrlId);

    @Query("""
        SELECT COALESCE(c.referrer, 'Direct') as label, COUNT(c) as count
        FROM ClickAnalytics c
        WHERE c.shortUrl.id = :shortUrlId
        GROUP BY COALESCE(c.referrer, 'Direct')
        ORDER BY count DESC
    """)
    List<AnalyticsAggregation> getReferrerStats(@Param("shortUrlId") Long shortUrlId);

    @Query("""
        SELECT c FROM ClickAnalytics c
        WHERE c.shortUrl.id = :shortUrlId AND c.clickedAt >= :since
        ORDER BY c.clickedAt ASC
    """)
    List<ClickAnalytics> findRecentClicks(@Param("shortUrlId") Long shortUrlId, @Param("since") Instant since);

    interface AnalyticsAggregation {
        String getLabel();
        Long getCount();
    }
}
