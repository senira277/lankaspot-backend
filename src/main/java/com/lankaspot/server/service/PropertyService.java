package com.lankaspot.server.service;

import com.lankaspot.server.repository.ApplicationPropertiesRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PropertyService {
    private final ApplicationPropertiesRepository applicationPropertiesRepository;

    public String getValue(String key) {
       com.lankaspot.server.model.ApplicationProperties property =
               applicationPropertiesRepository.findByKey(key);
       return property == null ? null : property.getValue();
    }
}
