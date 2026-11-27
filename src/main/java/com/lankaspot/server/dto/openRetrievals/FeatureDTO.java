package com.lankaspot.server.dto.openRetrievals;

import com.lankaspot.server.enums.FeatureValueType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeatureDTO {
    private Long id;
    private String name;
    private FeatureValueType valueType;
    private String unit;
    private String icon;

    public FeatureDTO(Long id, String name, FeatureValueType valueType, String unit) {
        this(id, name, valueType, unit, null);
    }
}
