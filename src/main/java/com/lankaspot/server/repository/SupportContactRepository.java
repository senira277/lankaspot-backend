package com.lankaspot.server.repository;

import com.lankaspot.server.model.SupportContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SupportContactRepository extends JpaRepository<SupportContact, Long> {
    @Query("SELECT s FROM SupportContact s WHERE s.supportContactStatus = com.lankaspot.server.enums.SupportContactStatus.ACTIVE")
    List<SupportContact> findActiveSupportContacts();
}
