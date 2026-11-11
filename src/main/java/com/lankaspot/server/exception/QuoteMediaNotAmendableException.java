package com.lankaspot.server.exception;

import org.springframework.http.HttpStatus;

public class QuoteMediaNotAmendableException extends LankaspotException {
    public static final String CODE = "QUOTE_MEDIA_NOT_AMENDABLE";
    public QuoteMediaNotAmendableException(Long quoteRef, String status) {
        super(HttpStatus.CONFLICT, CODE, "This Quote's media is not amendable. Quote ID: " + quoteRef + ", Status: " + status + ", Please Contact Support if required.");
    }
}
