
package com.lankaspot.server.service.kafka;

import com.lankaspot.server.event.model.cacheInvalidation.CacheInvalidationEvent;
import com.lankaspot.server.enums.ProcessedEventStatus;
import com.lankaspot.server.model.ProcessedEvent;
import com.lankaspot.server.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventOrchestration {

    private final ProcessedEventRepository processedEventsRepository;


    @Transactional
    public boolean initiate(CacheInvalidationEvent event) {
        if (event == null || event.id() == null) {
            log.warn("Rejecting event because event or event ID is missing");
            return false;
        }

        int inserted = processedEventsRepository.insertIfAbsent(event.id());

        if (inserted == 0) {
            log.info(
                    "Duplicate event rejected. eventId={}",
                    event.id()
            );
            handleDuplicate(event.id());
            return false;
        }

        log.info(
                "Event accepted for processing. eventId={}",
                event.id()
        );

        return true;
    }

    @Transactional
    public void handleDuplicate(UUID eventId) {
        ProcessedEvent event = processedEventsRepository
                .findByEventId(eventId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Duplicate event record not found: " + eventId
                ));

        event.setStatus(ProcessedEventStatus.REJECTED_DUPLICATE);
        event.setProcessedAt(Instant.now());
        event.setErrorMessage("Duplicate event received. Event ID already exists.");

        processedEventsRepository.save(event);

        log.warn(
                "Duplicate event handled. eventId={}, processedEventId={}",
                event.getEventId(),
                event.getId()
        );
    }

    @Transactional
    public void markSuccess(UUID eventId) {
        ProcessedEvent event = getEventByEventId(eventId);

        event.setStatus(ProcessedEventStatus.SUCCESS);
        event.setProcessedAt(Instant.now());
        event.setErrorMessage(null);

        processedEventsRepository.save(event);

        log.info(
                "Event processing completed successfully. processedEventId={}, eventId={}",
                event.getId(),
                event.getEventId()
        );
    }

    @Transactional
    public void markFailed(UUID processedEventId, Exception exception) {
        ProcessedEvent event = getEventByEventId(processedEventId);

        event.setStatus(ProcessedEventStatus.FAILED);
        event.setProcessedAt(Instant.now());
        event.setErrorMessage(truncateErrorMessage(exception));

        processedEventsRepository.save(event);

        log.error(
                "Event processing failed. processedEventId={}, eventId={}",
                event.getId(),
                event.getEventId(),
                exception
        );
    }

    private ProcessedEvent getEvent(Long processedEventId) {
        return processedEventsRepository.findById(processedEventId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Processed event not found: " + processedEventId
                ));
    }

    private ProcessedEvent getEventByEventId(UUID processedEventId) {
        return processedEventsRepository.findByEventId(processedEventId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Processed event not found: " + processedEventId
                ));
    }

    private String truncateErrorMessage(Exception exception) {
        if (exception == null) {
            return "Unknown processing error";
        }

        String message = exception.getMessage();

        if (message == null || message.isBlank()) {
            message = exception.getClass().getSimpleName();
        }

        return message.length() > 2000
                ? message.substring(0, 2000)
                : message;
    }
}
