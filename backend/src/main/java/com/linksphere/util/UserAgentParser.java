package com.linksphere.util;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.regex.Pattern;

public final class UserAgentParser {

    private static final Pattern BOT_PATTERN = Pattern.compile("(?i)(bot|crawl|spider|slurp|facebookexternalhit|curl|wget)");

    private UserAgentParser() {}

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeviceInfo {
        private String deviceType;
        private String browser;
        private String operatingSystem;
    }

    public static DeviceInfo parse(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return DeviceInfo.builder()
                    .deviceType("Unknown")
                    .browser("Unknown")
                    .operatingSystem("Unknown")
                    .build();
        }

        String ua = userAgent.toLowerCase();

        // 1. Bot check
        if (BOT_PATTERN.matcher(ua).find()) {
            return DeviceInfo.builder()
                    .deviceType("Bot")
                    .browser("Bot")
                    .operatingSystem("Bot")
                    .build();
        }

        // 2. Device Type
        String deviceType = "Desktop";
        if (ua.contains("ipad") || ua.contains("tablet") || (ua.contains("android") && !ua.contains("mobile"))) {
            deviceType = "Tablet";
        } else if (ua.contains("mobile") || ua.contains("iphone") || ua.contains("ipod") || ua.contains("android")) {
            deviceType = "Mobile";
        }

        // 3. Operating System
        String os = "Other";
        if (ua.contains("windows")) {
            os = "Windows";
        } else if (ua.contains("mac os") || ua.contains("macintosh")) {
            os = "macOS";
        } else if (ua.contains("iphone") || ua.contains("ipad") || ua.contains("ipod")) {
            os = "iOS";
        } else if (ua.contains("android")) {
            os = "Android";
        } else if (ua.contains("linux")) {
            os = "Linux";
        }

        // 4. Browser
        String browser = "Other";
        if (ua.contains("edg/") || ua.contains("edge/")) {
            browser = "Edge";
        } else if (ua.contains("opr/") || ua.contains("opera/")) {
            browser = "Opera";
        } else if (ua.contains("chrome") && !ua.contains("chromium")) {
            browser = "Chrome";
        } else if (ua.contains("safari") && !ua.contains("chrome")) {
            browser = "Safari";
        } else if (ua.contains("firefox")) {
            browser = "Firefox";
        }

        return DeviceInfo.builder()
                .deviceType(deviceType)
                .browser(browser)
                .operatingSystem(os)
                .build();
    }
}
