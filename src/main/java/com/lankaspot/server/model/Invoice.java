package com.lankaspot.server.model;

import com.lankaspot.server.enums.InvoiceStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "invoices",
        indexes = {
                @Index(name = "idx_invoices_advertiser", columnList = "advertiser_id"),
                @Index(name = "idx_invoices_quote_ref", columnList = "quote_ref")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_invoice_number", columnNames = "invoice_number"),
                @UniqueConstraint(name = "uq_invoice_registry", columnNames = "payment_registry_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Customer-facing number, e.g. LS-INV-2026-000123. Set right after the first save (see service note). */
    @Column(name = "invoice_number", length = 30)
    private String invoiceNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InvoiceStatus status = InvoiceStatus.ISSUED;

    /* ---------- Links ---------- */

    /** The checkout attempt this invoice came from. One invoice per successful attempt. */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_registry_id", nullable = false, updatable = false)
    private PaymentRegistry paymentRegistry;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "advertisement_id")
    private Advertisement advertisement;

    /** Keycloak user id, same as AdQuote.advertiserId. */
    @Column(name = "advertiser_id", nullable = false, updatable = false)
    private UUID advertiserId;

    @Column(name = "quote_ref", nullable = false, updatable = false)
    private Long quoteRef;

    /** PayHere order_id of the attempt, kept for support lookups. */
    @Column(name = "order_id", nullable = false, updatable = false, length = 64)
    private String orderId;

    /** PayHere payment_id. */
    @Column(name = "provider_reference", length = 100)
    private String providerReference;
    @Column(name = "download_url", length = 500)
    private String downloadUrl;

    /* ---------- Billed party and subject (frozen at issue time) ---------- */

    @Column(name = "billing_name", nullable = false, length = 200)
    private String billingName;

    @Column(name = "billing_email", nullable = false, length = 255)
    private String billingEmail;

    @Column(name = "billing_phone", length = 30)
    private String billingPhone;

    @Column(name = "listing_title", nullable = false, length = 200)
    private String listingTitle;

    /* ---------- Amounts ---------- */

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "subtotal", nullable = false, precision = 15, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "discount_code", length = 50)
    private String discountCode;

    @Column(name = "discount_percentage", precision = 5, scale = 2)
    private BigDecimal discountPercentage;

    @Column(name = "discount_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    /** What the customer actually paid. */
    @Column(name = "total_paid", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalPaid;

    /* ---------- Dates ---------- */

    @Column(name = "paid_at", nullable = false)
    private LocalDateTime paidAt;

    @Column(name = "issued_at", nullable = false, updatable = false)
    private LocalDateTime issuedAt;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<InvoiceLine> lines = new ArrayList<>();

    @PrePersist
    void onCreate() {
        if (issuedAt == null) issuedAt = LocalDateTime.now();
    }

    public void addLine(InvoiceLine line) {
        line.setInvoice(this);
        line.setSortOrder(lines.size());
        lines.add(line);
    }
}