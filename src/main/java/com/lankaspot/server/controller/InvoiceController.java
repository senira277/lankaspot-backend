
package com.lankaspot.server.controller;

import com.lankaspot.server.dto.advertiser.invoice.InvoiceResponse;
import com.lankaspot.server.response.LankaspotResponse;
import com.lankaspot.server.service.InvoiceDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/invoices")
public class InvoiceController {

    private final InvoiceDocumentService invoiceDocumentService;

    @GetMapping("/{invoiceNumber}/pdf")
    public ResponseEntity<byte[]> downloadInvoice(@PathVariable String invoiceNumber, Authentication authentication) throws IOException {

        UUID advertiserId = UUID.fromString(authentication.getName());

        byte[] pdf = invoiceDocumentService.generateForAdvertiser(
                invoiceNumber,
                advertiserId
        );

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\""
                                + invoiceNumber + ".pdf\""
                )
                .body(pdf);
    }

    @GetMapping("/get-invoice-by-order")
    public ResponseEntity<LankaspotResponse<InvoiceResponse>> getInvoiceByOrder(
            @RequestParam String orderId, Authentication authentication) {
        UUID advertiserId = UUID.fromString(authentication.getName());
        return ResponseEntity.ok(invoiceDocumentService.getForAdvertiser(orderId, advertiserId));
    }

}
