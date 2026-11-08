package com.lankaspot.server.exception;

import org.springframework.http.HttpStatus;

public class PolicyNotFoundException extends LankaspotException {
    public static final String CODE = "POLICY_NOT_FOUND";
    public PolicyNotFoundException(String advertisementRef) {

        super(HttpStatus.NOT_FOUND, CODE, "Advertisement not found. (advertisementRef=" + advertisementRef + ")");
    }
}
