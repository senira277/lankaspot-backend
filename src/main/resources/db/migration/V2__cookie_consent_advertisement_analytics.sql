CREATE TABLE IF NOT EXISTS cookie_consents (
    id BIGSERIAL PRIMARY KEY,
    consent_id VARCHAR(64) NOT NULL,
    user_id BIGINT,
    necessary BOOLEAN NOT NULL DEFAULT TRUE,
    analytics BOOLEAN NOT NULL DEFAULT FALSE,
    marketing BOOLEAN NOT NULL DEFAULT FALSE,
    policy_version VARCHAR(40) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_cookie_consent_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_cookie_consent_id_created ON cookie_consents(consent_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_cookie_consent_user_created ON cookie_consents(user_id, created_at DESC);

CREATE TABLE IF NOT EXISTS analytics_events (
    id BIGSERIAL PRIMARY KEY,
    event_id UUID NOT NULL UNIQUE,
    advertisement_id BIGINT NOT NULL,
    event_type VARCHAR(48) NOT NULL,
    visitor_key_hash VARCHAR(64) NOT NULL,
    session_id VARCHAR(64),
    occurred_at TIMESTAMPTZ NOT NULL,
    received_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    duration_seconds INTEGER,
    CONSTRAINT fk_analytics_event_advertisement FOREIGN KEY (advertisement_id) REFERENCES advertisements(id)
);

CREATE INDEX IF NOT EXISTS idx_analytics_events_advertisement_time ON analytics_events(advertisement_id, occurred_at);
CREATE INDEX IF NOT EXISTS idx_analytics_events_visitor_time ON analytics_events(visitor_key_hash, occurred_at);

CREATE TABLE IF NOT EXISTS analytics_unique_views (
    id BIGSERIAL PRIMARY KEY,
    advertisement_id BIGINT NOT NULL,
    visitor_key_hash VARCHAR(64) NOT NULL,
    view_date DATE NOT NULL,
    CONSTRAINT uk_analytics_unique_view UNIQUE (advertisement_id, visitor_key_hash, view_date),
    CONSTRAINT fk_analytics_unique_advertisement FOREIGN KEY (advertisement_id) REFERENCES advertisements(id)
);

CREATE TABLE IF NOT EXISTS advertisement_analytics_daily (
    id BIGSERIAL PRIMARY KEY,
    advertisement_id BIGINT NOT NULL,
    analytics_date DATE NOT NULL,
    total_views BIGINT NOT NULL DEFAULT 0,
    unique_views BIGINT NOT NULL DEFAULT 0,
    contact_retrievals BIGINT NOT NULL DEFAULT 0,
    engaged_seconds BIGINT NOT NULL DEFAULT 0,
    engaged_sessions BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_advertisement_analytics_daily UNIQUE (advertisement_id, analytics_date),
    CONSTRAINT fk_analytics_daily_advertisement FOREIGN KEY (advertisement_id) REFERENCES advertisements(id)
);
