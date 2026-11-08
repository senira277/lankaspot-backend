package com.lankaspot.server.event.model.cacheInvalidation;

import java.time.Instant;
import java.util.UUID;

public record CacheInvalidationEvent(
        UUID id,
        Instant publishedAt,
        CacheInvalidationBody body
) {
}
