package com.lankaspot.server.dto.advertiser.invoice;

import com.lankaspot.server.model.Invoice;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record InvoiceResponse(
        String invoiceNumber,
        String status,
        String orderId,
        String providerReference,
        String billingName,
        String billingEmail,
        String billingPhone,
        String listingTitle,
        String currency,
        BigDecimal subtotal,
        String discountCode,
        BigDecimal discountPercentage,
        BigDecimal discountAmount,
        BigDecimal totalPaid,
        LocalDateTime paidAt,
        LocalDateTime issuedAt,
        String advertisementRef,
        List<InvoiceLineResponse> lines
) {
    public static InvoiceResponse from(Invoice i) {
        return new InvoiceResponse(
                i.getInvoiceNumber(),
                i.getStatus().name(),
                i.getOrderId(),
                i.getProviderReference(),
                i.getBillingName(),
                i.getBillingEmail(),
                i.getBillingPhone(),
                i.getListingTitle(),
                i.getCurrency(),
                i.getSubtotal(),
                i.getDiscountCode(),
                i.getDiscountPercentage(),
                i.getDiscountAmount(),
                i.getTotalPaid(),
                i.getPaidAt(),
                i.getIssuedAt(),
                i.getAdvertisement() == null
                        ? null
                        : i.getAdvertisement().getAdvertisementRef().toString(),
                i.getLines().stream()
                        .map(l -> new InvoiceLineResponse(
                                l.getKind().name(), l.getName(), l.getDetail(), l.getAmount()))
                        .toList());
    }
}
