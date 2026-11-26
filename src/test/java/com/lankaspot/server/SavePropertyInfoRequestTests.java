package com.lankaspot.server;

import com.lankaspot.server.dto.advertiser.savePropertyInfo.SavePropertyInfoRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SavePropertyInfoRequestTests {

    private static Validator validator;
    private static jakarta.validation.ValidatorFactory validatorFactory;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @Test
    void coordinatesMustBeAbsentWhenConsentIsFalse() {
        SavePropertyInfoRequest request = new SavePropertyInfoRequest();
        request.setLatitude(new BigDecimal("6.9271000"));
        request.setLongitude(new BigDecimal("79.8612000"));

        assertTrue(hasLocationConsentViolation(request));
    }

    @Test
    void bothCoordinatesAreRequiredWhenConsentIsTrue() {
        SavePropertyInfoRequest request = new SavePropertyInfoRequest();
        request.setPropertyLocationConsent(true);
        request.setLatitude(new BigDecimal("6.9271000"));

        assertTrue(hasLocationConsentViolation(request));
    }

    @Test
    void coordinatesAreValidOnlyWithConsentAndWithinGeographicBounds() {
        SavePropertyInfoRequest request = new SavePropertyInfoRequest();
        request.setPropertyLocationConsent(true);
        request.setLatitude(new BigDecimal("6.9271000"));
        request.setLongitude(new BigDecimal("79.8612000"));

        assertFalse(hasLocationConsentViolation(request));

        request.setLatitude(new BigDecimal("91"));
        assertTrue(validator.validate(request).stream()
                .anyMatch(violation -> "latitude".equals(violation.getPropertyPath().toString())));
    }

    private boolean hasLocationConsentViolation(SavePropertyInfoRequest request) {
        return validator.validate(request).stream()
                .anyMatch(violation ->
                        "propertyLocationConsentDataConsistent"
                                .equals(violation.getPropertyPath().toString()));
    }
}
