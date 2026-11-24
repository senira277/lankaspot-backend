package com.lankaspot.server.repository;

import com.lankaspot.server.model.Contact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContactRepository extends JpaRepository<Contact, Long> {
    List<Contact> findByUser_Id(Long userId);
    List<Contact> findByUser_KeycloakUserId(String keycloakUserId);
    boolean existsByUser_IdAndPhoneNumber(Long userId, String phoneNumber);
    boolean existsByUser_KeycloakUserIdAndPhoneNumber(String keycloakUserId, String phoneNumber);
    List<Contact> findByUser_KeycloakUserIdAndStatus(String keycloakUserId, com.lankaspot.server.enums.ContactStatus status);
    List<Contact> findByUser_IdAndStatus(Long userId, com.lankaspot.server.enums.ContactStatus status);
}
