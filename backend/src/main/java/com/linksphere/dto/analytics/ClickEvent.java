package com.linksphere.dto.analytics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClickEvent {
    private Long shortUrlId;
    private String referrer;
    private String userAgent;
    private String ipAddress;
}
