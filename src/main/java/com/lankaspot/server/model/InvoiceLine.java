package com.lankaspot.server.model;

import com.lankaspot.server.enums.InvoiceLineKind;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "invoice_lines")
@Getter
@Setter
@NoArgsConstructor
public class InvoiceLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 20)
    private InvoiceLineKind kind;

    /** SubscriptionPlan.id or AdvertisementTier.id at purchase time. Plain value, not an FK, so catalog edits never touch history. */
    @Column(name = "reference_id", nullable = false)
    private Long referenceId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;
    
    /** Shown under the name, e.g. "Listing visible for 1 month" or "Runs for 7 days". */
    @Column(name = "detail", length = 200)
    private String detail;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
}