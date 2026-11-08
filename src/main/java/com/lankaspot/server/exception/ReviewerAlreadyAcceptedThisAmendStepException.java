package com.lankaspot.server.exception;

import org.springframework.http.HttpStatus;

public class ReviewerAlreadyAcceptedThisAmendStepException extends LankaspotException {
    public static final String CODE = "AMEND_STEP_ALREADY_ACCEPTED";

    public ReviewerAlreadyAcceptedThisAmendStepException(Long quoteRef, String status) {
        super(HttpStatus.CONFLICT, CODE, "This Amend Step is already accepted. Quote ID: " + quoteRef + ", Review Status: " + status);
    }
}
