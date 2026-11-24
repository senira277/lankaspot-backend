package com.lankaspot.server.dto.analytics;

import jakarta.validation.constraints.NotNull;

public record ConsentRequest(
        @NotNull Boolean necessary,
        @NotNull Boolean analytics,
        @NotNull Boolean marketing
) {
}

