package com.lankaspot.server.repository;

import com.lankaspot.server.enums.EntityMappingType;
import com.lankaspot.server.model.EntityMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.OptionalLong;

public interface EntityMappingRepository extends JpaRepository<EntityMapping, Long> {

    Optional<EntityMapping> findByQuote_Id(Long quoteId);

    Optional<EntityMapping> findIdByPolicy_IdAndMappingType(Long policyId, EntityMappingType mappingType);

    boolean existsByQuote_IdAndMappingType(
            Long quoteId,
            EntityMappingType mappingType
    );
}