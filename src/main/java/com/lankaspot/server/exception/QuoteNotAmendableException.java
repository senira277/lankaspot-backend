package com.lankaspot.server.exception;

import org.springframework.http.HttpStatus;

public class QuoteNotAmendableException extends LankaspotException {
    public static final String CODE = "QUOTE_NOT_AMENDABLE";

    public QuoteNotAmendableException(Long quoteRef, String status) {
        super(HttpStatus.CONFLICT, CODE, "This Quote is not amendable. Quote ID: " + quoteRef + ", Status: " + status + ", Please Contact Support if required.");
    }
}
