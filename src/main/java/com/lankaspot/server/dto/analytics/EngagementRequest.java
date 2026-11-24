package com.lankaspot.server.dto.analytics;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EngagementRequest(
        @NotBlank @Size(max = 64) String sessionId
) {
}

