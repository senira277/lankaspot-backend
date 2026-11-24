package com.lankaspot.server.dto.analytics;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record HeartbeatRequest(
        @NotBlank @Size(max = 64) String sessionId,
        @Min(1) @Max(30) int intervalSeconds
) {
}

