package com.lankaspot.server.enums;

public enum ProcessedEventStatus {
    ACCEPTED,
    IN_PROGRESS,
    SUCCESS,
    FAILED,
    REJECTED_DUE_TO_CONCURRENT_EXECUTION,
    STALE,
    REJECTED_DUPLICATE
}