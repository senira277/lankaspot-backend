package com.lankaspot.server.event.consumer;

import com.lankaspot.server.event.model.EmailSending.EmailEvent;
import com.lankaspot.server.model.Invoice;
import com.lankaspot.server.repository.InvoiceRepository;
import com.lankaspot.server.service.AdvertisementService;
import com.lankaspot.server.service.email.EmailService;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailSendingConsumer {
    private final EmailService emailService;
    private final AdvertisementService advertisementService;
    private final InvoiceRepository invoiceRepository;

    public void consume(EmailEvent event){
        try {
            log.info("consuming Email Sending Event: {}", event);
            emailService.send(
                    advertisementService.getAdvertiserEmailByAdvertisementRef(event.body().advertisementRef()),
                    "ADVERTISEMENT PurchasedD Successfully",
                    event.body().eventType(),
                    createMap(event)
            );
        } catch (MessagingException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private Map<String, Object> createMap(EmailEvent event) {
        return event.body().optionalArgs();
    }



}
