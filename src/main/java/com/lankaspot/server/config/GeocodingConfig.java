package com.lankaspot.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class GeocodingConfig {
    @Bean
    public RestClient geoapifyRestClient(
            GeocodingProperties properties
    ) {
        return RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .build();
    }
}
