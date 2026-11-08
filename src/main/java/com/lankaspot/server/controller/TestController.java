package com.lankaspot.server.controller;

import com.lankaspot.server.enums.EmailEventType;
import com.lankaspot.server.event.model.EmailSending.EmailEventBody;
import com.lankaspot.server.service.email.EmailService;
import com.lankaspot.server.service.kafka.EmailSendingProducer;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/test/public")
@RequiredArgsConstructor
public class TestController {
    private final EmailService emailService;
    private final EmailSendingProducer emailSendingProducer;

    @GetMapping("/send-email")
    public void sendEmail() throws MessagingException {
        //create test email
        Map<String, Object> variables = new HashMap<>();

        variables.put("name", "Test User");
        variables.put("verificationLink", "https://lankaspot.lk/verify/test-token");

        UUID uuid = UUID.randomUUID();

        //kafka
        emailSendingProducer.publish(
                new EmailEventBody(
                        EmailEventType.ADVERTISEMENT_PURCHASED_SUCCESS,
                        uuid,
                        variables
                )
        );

//        emailService.send(
//                "senira277@gmail.com",
//                "Lankaspot Test Email",
//                EmailEventType.TEST,
//                variables
//        );

    }

}