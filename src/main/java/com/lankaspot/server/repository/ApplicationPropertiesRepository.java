package com.lankaspot.server.repository;

import com.lankaspot.server.model.ApplicationProperties;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationPropertiesRepository extends JpaRepository<ApplicationProperties, Long> {
    ApplicationProperties findByKey(String key);
    List<ApplicationProperties> findByKeyStartingWith(String prefix);
}
