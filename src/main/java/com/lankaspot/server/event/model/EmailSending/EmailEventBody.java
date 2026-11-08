package com.lankaspot.server.event.model.EmailSending;

import com.lankaspot.server.enums.EmailEventType;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public record EmailEventBody (
        EmailEventType eventType,
        UUID advertisementRef,
        Map<String, Object> optionalArgs

){
}
