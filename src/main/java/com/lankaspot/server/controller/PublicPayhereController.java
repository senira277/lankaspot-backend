package com.lankaspot.server.controller;

import com.lankaspot.server.exception.InvalidPayhereNotificationException;
import com.lankaspot.server.service.payment.PayhereNotifyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/public/payhere")
@Slf4j
@RequiredArgsConstructor
public class PublicPayhereController {
    private final PayhereNotifyService notifyService;

    @PostMapping(value = "/notify-payhere", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<String> notifyPayhere(@RequestParam Map<String, String> params) {
        try {
            log.info("Received PayHere notification from Payhere: {}", params);
            log.info("Saving Payhere notify response...");
            notifyService.insertNotification(params);

            log.info("Successfully saved Payhere notify response.");
            log.info("Processing Registry Update...");
            notifyService.updateRegistry(params);

            log.info("Successfully saved Registry update.");
            log.info("Processing Ledgers...");
            notifyService.handleLedger(params.get("order_id"));

            log.info("Successfully processed Ledgers!");
            log.info("Processing Fulfillment...");
            Map<String, Object> emailArgs = notifyService.handleFulfilment(params);

            log.info("Successfully processed fulfillment!");
            log.info("Handling Email Services...");
            notifyService.notifyFulfillment(emailArgs);

            log.info("Successfully handled PayHere notification.");
            return ResponseEntity.ok("OK");
        } catch (InvalidPayhereNotificationException e) {
            log.warn("Rejected PayHere notify: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Invalid");
        }
    }


}
