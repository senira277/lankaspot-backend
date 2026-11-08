package com.lankaspot.server.service.email;

import com.lankaspot.server.enums.EmailEventType;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final TemplateEngine templateEngine;
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String from;

    public String generateBody(EmailEventType emailType, Map<String, Object> variables) {
        Context context = new Context(Locale.ENGLISH);
        context.setVariables(variables);
        return templateEngine.process(resolveTemplate(emailType), context);
    }

    public void send(
            String to,
            String subject,
            EmailEventType emailType,
            Map<String, Object> variables
    ) throws MessagingException {
        String html = generateBody(emailType, variables);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        helper.setFrom(from);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(html, true);

        ClassPathResource logo = new ClassPathResource("static/images/lankaspot-logo.png");
        if (logo.exists()) {
            helper.addInline("lankaspot-logo", logo);
        }

        mailSender.send(message);
        log.info("Email sent. to={}, subject={}, emailType={}", to, subject, emailType);
    }

    private String resolveTemplate(EmailEventType emailType) {
        return switch (emailType) {
            case ADVERTISEMENT_PURCHASED_SUCCESS -> "adPurchaseSuccess";
            case TEST ->  "testEmail";

            //default
            default -> throw new IllegalArgumentException(
                    "No email template configured for type: " + emailType
            );
        };
    }
}