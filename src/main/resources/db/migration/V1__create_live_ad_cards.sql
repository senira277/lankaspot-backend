CREATE TABLE IF NOT EXISTS live_ad_cards (
    id BIGSERIAL PRIMARY KEY,
    advertisement_id UUID NOT NULL UNIQUE,
    category_id BIGINT NOT NULL,
    category_name VARCHAR(100) NOT NULL,
    city_id BIGINT NOT NULL,
    city_name VARCHAR(100) NOT NULL,
    district_id BIGINT NOT NULL,
    district_name VARCHAR(100) NOT NULL,
    functional_group_id BIGINT,
    functional_group_name VARCHAR(100),
    title VARCHAR(200) NOT NULL,
    nearest_landmark VARCHAR(100),
    town VARCHAR(100),
    price NUMERIC(15, 2),
    slug VARCHAR(250) NOT NULL,
    quote_ref VARCHAR(100) NOT NULL,
    cover_url TEXT,
    sort_order INTEGER NOT NULL DEFAULT 0,
    queue_number BIGINT NOT NULL DEFAULT 0,
    expires_at TIMESTAMP,
    CONSTRAINT fk_live_ad_card_advertisement
        FOREIGN KEY (advertisement_id) REFERENCES advertisements(advertisement_ref)
);

CREATE INDEX IF NOT EXISTS idx_live_ad_cards_category ON live_ad_cards(category_id);
CREATE INDEX IF NOT EXISTS idx_live_ad_cards_city ON live_ad_cards(city_id);
CREATE INDEX IF NOT EXISTS idx_live_ad_cards_district ON live_ad_cards(district_id);
CREATE INDEX IF NOT EXISTS idx_live_ad_cards_listing_order
    ON live_ad_cards(sort_order, queue_number, advertisement_id);
CREATE INDEX IF NOT EXISTS idx_live_ad_cards_expires_at ON live_ad_cards(expires_at);
