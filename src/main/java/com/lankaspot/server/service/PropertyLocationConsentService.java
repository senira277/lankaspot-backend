package com.lankaspot.server.service;

import com.lankaspot.server.model.ApplicationProperties;
import com.lankaspot.server.repository.ApplicationPropertiesRepository;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PropertyLocationConsentService {

    private static final String CONSENT_PROPERTY_PREFIX = "PropertyLocationConsent_";
    private static final Pattern CONSENT_PROPERTY =
            Pattern.compile("^PropertyLocationConsent_(\\d+)$");

    private final ApplicationPropertiesRepository applicationPropertiesRepository;
    private final PropertyService propertyService;

    public PropertyLocationConsentService(
            ApplicationPropertiesRepository applicationPropertiesRepository,
            PropertyService propertyService
    ) {
        this.applicationPropertiesRepository = applicationPropertiesRepository;
        this.propertyService = propertyService;
    }

    public Consent latestConsent() {
        List<ApplicationProperties> configuredProperties =
                applicationPropertiesRepository.findByKeyStartingWith(CONSENT_PROPERTY_PREFIX);

        ConsentProperty latestProperty = configuredProperties.stream()
                .map(this::toConsentProperty)
                .filter(property -> property != null)
                .max(Comparator.comparing(ConsentProperty::numericVersion)
                        .thenComparing(ConsentProperty::version))
                .orElseThrow(() -> new IllegalStateException(
                        "No PropertyLocationConsent_<number> property is configured in m_application_properties"
                ));

        String message = propertyService.getValue(latestProperty.key());
        if (message == null || message.isBlank()) {
            throw new IllegalStateException(
                    "The latest property location consent message is empty"
            );
        }

        return new Consent(latestProperty.version(), message);
    }

    private ConsentProperty toConsentProperty(ApplicationProperties property) {
        Matcher matcher = CONSENT_PROPERTY.matcher(property.getKey());
        if (!matcher.matches()) {
            return null;
        }

        String version = matcher.group(1);
        return new ConsentProperty(
                property.getKey(),
                version,
                new BigInteger(version)
        );
    }

    private record ConsentProperty(String key, String version, BigInteger numericVersion) {
    }

    public record Consent(String version, String message) {
    }
}
