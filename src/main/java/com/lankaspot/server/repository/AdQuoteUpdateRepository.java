package com.lankaspot.server.repository;

import com.lankaspot.server.model.AdQuote;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;

/**
 * Explicit SQL updates for a quote that is still editable. AdQuote's columns are mapped
 * updatable = false, so changing the entity and saving it would silently write nothing; these
 * queries write the columns directly instead.
 */
public interface AdQuoteUpdateRepository extends Repository<AdQuote, Long> {

    /**
     * Updates the step-1 fields and increments the version, only if the quote is still at
     * expectedVersion and still has an editable status (guards against a concurrent edit or purchase).
     *
     * @return number of rows updated: 1 on success, 0 if the guard failed
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            update ad_quotes
               set category_id = :categoryId,
                   title = :title,
                   slug = :slug,
                   description = cast(:description as text),
                   price = :price,
                   city_id = :cityId,
                   town = :town,
                   nearest_landmark = :nearestLandmark,
                   distance_to_nearest_landmark_in_km = :distanceToNearestLandmarkInKm,
                   address = cast(:address as text),
                   latitude = :latitude,
                   longitude = :longitude,
                   property_location_consent = :propertyLocationConsent,
                   property_location_consent_text = cast(:propertyLocationConsentText as text),
                   property_location_consent_version = :propertyLocationConsentVersion,
                   version = version + 1
             where id = :id
               and version = :expectedVersion
               and status in (:editableStatuses)
            """, nativeQuery = true)
    int updatePropertyInfo(@Param("id") Long id,
                           @Param("expectedVersion") Integer expectedVersion,
                           @Param("editableStatuses") Collection<String> editableStatuses,
                           @Param("categoryId") Long categoryId,
                           @Param("title") String title,
                           @Param("slug") String slug,
                           @Param("description") String description,
                           @Param("price") BigDecimal price,
                           @Param("cityId") Long cityId,
                           @Param("town") String town,
                           @Param("nearestLandmark") String nearestLandmark,
                           @Param("distanceToNearestLandmarkInKm") BigDecimal distanceToNearestLandmarkInKm,
                           @Param("address") String address,
                           @Param("latitude") BigDecimal latitude,
                           @Param("longitude") BigDecimal longitude,
                           @Param("propertyLocationConsent") boolean propertyLocationConsent,
                           @Param("propertyLocationConsentText") String propertyLocationConsentText,
                           @Param("propertyLocationConsentVersion") String propertyLocationConsentVersion);

    @Modifying(flushAutomatically = true)
    @Query(value = "delete from ad_quote_features where ad_quote_id = :quoteId", nativeQuery = true)
    int deleteFeatures(@Param("quoteId") Long quoteId);

    @Modifying
    @Query(value = """
            insert into ad_quote_features (ad_quote_id, feature_id, value)
            values (:quoteId, :featureId, cast(:value as text))
            """, nativeQuery = true)
    int insertFeature(@Param("quoteId") Long quoteId,
                      @Param("featureId") Long featureId,
                      @Param("value") String value);
}