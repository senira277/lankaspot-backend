
package com.lankaspot.server.service;

import com.lankaspot.server.dto.advertiser.invoice.InvoiceResponse;
import com.lankaspot.server.model.Invoice;
import com.lankaspot.server.repository.InvoiceRepository;
import com.lankaspot.server.response.LankaspotResponse;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InvoiceDocumentService {

    private final InvoiceRepository invoiceRepository;
    private final InvoicePdfService invoicePdfService;


    @Transactional(readOnly = true)
    public byte[] generateForAdvertiser(String invoiceNumber, UUID advertiserId
    ) throws IOException {

        Invoice invoice = invoiceRepository
                .findForAdvertiser(invoiceNumber, advertiserId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Invoice not found")
                );

        return invoicePdfService.generateInvoice(invoice);
    }

    @Transactional(readOnly = true)
    public LankaspotResponse<InvoiceResponse> getForAdvertiser(String orderId, UUID advertiserId) {
        Invoice invoice = invoiceRepository
                .findByOrderIdAndAdvertiserId(orderId, advertiserId)
                .orElseThrow(() -> new EntityNotFoundException("Invoice not found"));
        return LankaspotResponse.success(InvoiceResponse.from(invoice));
    }
}
