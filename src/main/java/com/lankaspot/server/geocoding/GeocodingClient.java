package com.lankaspot.server.geocoding;

import com.lankaspot.server.config.GeocodingProperties;
import com.lankaspot.server.dto.location.GeoapifyGeocodingResponse;
import com.lankaspot.server.service.PropertyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class GeocodingClient {

    private final RestClient geoapifyRestClient;
    private final GeocodingProperties properties;
    private final PropertyService propertyService;
    private final ObjectMapper objectMapper;

    public GeoapifyGeocodingResponse autocomplete(String text) {

        String enabled = propertyService.getValue("GEOCODING_API_ENABLED");

        if (!"true".equalsIgnoreCase(enabled)) {
            throw new RuntimeException(
                    "Geocoding API is not enabled in the database."
            );
        }

        String response = geoapifyRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/geocode/autocomplete")
                        .queryParam("text", text)
                        .queryParam("format", "json")
                        .queryParam("limit", 5)
                        .queryParam("countrycodes", "lk")
                        .queryParam("apiKey", properties.getApiKey())
                        .build())
                .retrieve()
                .body(String.class);

        try {
            return objectMapper.readValue(
                    response,
                    GeoapifyGeocodingResponse.class
            );
        } catch (JacksonException e) {
            throw new RuntimeException(
                    "Failed to parse Geoapify geocoding response.",
                    e
            );
        }
    }
}