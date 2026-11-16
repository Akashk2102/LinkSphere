-- =============================================================================
-- Migration V1: Initial Schema for LinkSphere
-- =============================================================================

-- 1. Users Table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role VARCHAR(30) NOT NULL DEFAULT 'ROLE_USER',
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_users_email UNIQUE (email)
);

-- 2. Short URLs Table
CREATE TABLE short_urls (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    original_url TEXT NOT NULL,
    short_code VARCHAR(32) NOT NULL,
    title VARCHAR(255),
    is_custom_alias BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    expires_at TIMESTAMP WITH TIME ZONE,
    click_count BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_short_urls_code UNIQUE (short_code),
    CONSTRAINT fk_short_urls_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- Indexes for Short URLs
CREATE INDEX idx_short_urls_user_created ON short_urls (user_id, created_at DESC);
CREATE INDEX idx_short_urls_expires_at ON short_urls (expires_at) WHERE expires_at IS NOT NULL;

-- 3. Click Analytics Table
CREATE TABLE click_analytics (
    id BIGSERIAL PRIMARY KEY,
    short_url_id BIGINT NOT NULL,
    clicked_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    referrer VARCHAR(512),
    device_type VARCHAR(50),
    browser VARCHAR(50),
    operating_system VARCHAR(50),
    country_code VARCHAR(10),
    ip_hash VARCHAR(64),
    CONSTRAINT fk_analytics_short_url FOREIGN KEY (short_url_id) REFERENCES short_urls (id) ON DELETE CASCADE
);

-- Indexes for Analytics aggregation queries
CREATE INDEX idx_analytics_url_timestamp ON click_analytics (short_url_id, clicked_at DESC);
