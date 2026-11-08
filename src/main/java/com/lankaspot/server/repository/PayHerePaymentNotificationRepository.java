package com.lankaspot.server.repository;

import com.lankaspot.server.model.PayHerePaymentNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PayHerePaymentNotificationRepository extends JpaRepository<PayHerePaymentNotification, Long> {
    List<PayHerePaymentNotification> findByOrderIdOrderByReceivedAtDesc(
            String orderId);

    List<PayHerePaymentNotification> findByPaymentIdOrderByReceivedAtDesc(
            String paymentId);

    Boolean existsByOrderIdAndPaymentId(String orderId, String paymentId);
}
