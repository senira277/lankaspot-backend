package com.lankaspot.server.dto.advertiser.savePropertyInfo;

import com.lankaspot.server.dto.advertiser.advertisement.FunctionalGroupDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Step 1 of the advertiser wizard: everything except media.
 * The district is never sent; it is derived from the city.
 */
@Data
@NoArgsConstructor
public class SavePropertyInfoRequest {

    /** Set only when the advertiser re-saves step 1 for a quote they already created. */
    private Long quoteRef;

    @NotNull
    private Long categoryId;

    @NotBlank
    @Size(max = 200)
    private String title;

    @Size(max = 1200)
    private String description;

    @NotNull
    @Positive
    @Digits(integer = 13, fraction = 2)
    private BigDecimal price;

    @NotNull
    private Long cityId;

    @Valid
    private FunctionalGroupDTO functionalGroup;

    @NotBlank
    @Size(max = 100)
    private String town;

    @NotBlank
    @Size(max = 100)
    private String nearestLandmark;

    @Size(max = 1000)
    private String address;

    /** Checked features only. */
    @Valid
    private List<FeatureSelection> features = new ArrayList<>();

    @Data
    @NoArgsConstructor
    public static class FeatureSelection {

        @NotNull
        private Long featureId;

        /** Null for plain yes/no features, otherwise a number or text depending on the feature's valueType. */
        @Size(max = 100)
        private String value;
    }
}