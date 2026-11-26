ALTER TABLE ad_quotes
    ADD COLUMN latitude NUMERIC(10, 7),
    ADD COLUMN longitude NUMERIC(10, 7),
    ADD COLUMN property_location_consent BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN property_location_consent_text TEXT,
    ADD COLUMN property_location_consent_version VARCHAR(100);

ALTER TABLE ad_quotes
    ADD CONSTRAINT chk_ad_quotes_location_consent_coordinates
        CHECK (
            (property_location_consent
                AND latitude IS NOT NULL
                AND longitude IS NOT NULL)
            OR
            (NOT property_location_consent
                AND latitude IS NULL
                AND longitude IS NULL)
        ),
    ADD CONSTRAINT chk_ad_quotes_location_coordinate_ranges
        CHECK (
            (latitude IS NULL OR latitude BETWEEN -90 AND 90)
            AND (longitude IS NULL OR longitude BETWEEN -180 AND 180)
        );
