package com.lankaspot.server.config;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "geocoding")
@Getter
@Setter
public class GeocodingProperties {
    //Geoapify API key
    private String apiKey;
    private String baseUrl;

}
