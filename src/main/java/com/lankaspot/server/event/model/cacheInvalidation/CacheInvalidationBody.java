package com.lankaspot.server.event.model.cacheInvalidation;

import com.lankaspot.server.enums.CacheInvalidationEventType;

import java.util.UUID;

public record CacheInvalidationBody(
        CacheInvalidationEventType eventType,
        Long quoteId,
        UUID advertiserId,
        Long cacheVersion
) {
}
