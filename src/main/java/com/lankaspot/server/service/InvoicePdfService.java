
package com.lankaspot.server.service;

import com.lankaspot.server.model.Invoice;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoicePdfService {

    private final TemplateEngine templateEngine;



    public byte[] generateInvoice(Invoice invoice) throws IOException {
        Context context = new Context(Locale.ENGLISH);
        context.setVariable("invoice", invoice);
        context.setVariable("logoDataUri", loadLogo());

        String html = templateEngine.process("invoice", context);

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(html, null);
            builder.toStream(output);
            builder.run();
            log.info("Invoice PDF generated successfully");

            return output.toByteArray();
        } catch (Exception e) {
            throw new IOException("Failed to generate invoice PDF", e);
        }
    }

    private String loadLogo() throws IOException {
        ClassPathResource resource = new ClassPathResource(
                "static/images/lankaspot-logo.png"
        );

        if (!resource.exists()) {
            return null;
        }

        byte[] bytes;
        try (var input = resource.getInputStream()) {
            bytes = input.readAllBytes();
        }

        return "data:image/png;base64,"
                + Base64.getEncoder().encodeToString(bytes);
    }
}
