package com.lankaspot.server.exception;

import org.springframework.http.HttpStatus;

public class AdvertisementNotFoundException extends LankaspotException {
    public static final String CODE = "ADVERTISEMENT_NOT_FOUND";

    public AdvertisementNotFoundException(String advertisementRef) {
        super(HttpStatus.NOT_FOUND, CODE, "Advertisement not found. (advertisementRef=" + advertisementRef + ")");
    }
}
