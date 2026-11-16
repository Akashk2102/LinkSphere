package com.linksphere.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "click_analytics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClickAnalytics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "short_url_id", nullable = false)
    private ShortUrl shortUrl;

    @CreationTimestamp
    @Column(name = "clicked_at", nullable = false, updatable = false)
    private Instant clickedAt;

    @Column(length = 512)
    private String referrer;

    @Column(name = "device_type", length = 50)
    private String deviceType;

    @Column(length = 50)
    private String browser;

    @Column(name = "operating_system", length = 50)
    private String operatingSystem;

    @Column(name = "country_code", length = 10)
    private String countryCode;

    @Column(name = "ip_hash", length = 64)
    private String ipHash;
}
