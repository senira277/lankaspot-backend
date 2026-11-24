package com.lankaspot.server.repository;

import com.lankaspot.server.model.AdQuoteTier;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdQuoteTierRepository extends JpaRepository<AdQuoteTier, Long> {
    List<AdQuoteTier> findByAdQuote_Id(Long quoteId);



    //custom query
}
