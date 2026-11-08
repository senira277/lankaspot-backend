package com.lankaspot.server.repository;

import com.lankaspot.server.model.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findByPaymentRegistry_Id(Long registryId);
    Optional<Invoice> findByOrderIdAndAdvertiserId(String orderId, UUID advertiserId);
    Optional<Invoice> findByInvoiceNumberAndAdvertiserId(String invoiceNumber, UUID advertiserId);
    List<Invoice> findByAdvertiserIdOrderByIssuedAtDesc(UUID advertiserId);
    @Query("""
        SELECT DISTINCT i
        FROM Invoice i
        LEFT JOIN FETCH i.lines
        WHERE i.invoiceNumber = :invoiceNumber
          AND i.advertiserId = :advertiserId
        """)
    Optional<Invoice> findForAdvertiser(
            @Param("invoiceNumber") String invoiceNumber,
            @Param("advertiserId") UUID advertiserId
    );



}
