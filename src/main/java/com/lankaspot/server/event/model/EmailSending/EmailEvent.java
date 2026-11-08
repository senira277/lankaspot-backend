package com.lankaspot.server.event.model.EmailSending;

import java.time.Instant;
import java.util.UUID;

public record EmailEvent (
        UUID id,
        Instant publishedAt,
        EmailEventBody body
) {
}
