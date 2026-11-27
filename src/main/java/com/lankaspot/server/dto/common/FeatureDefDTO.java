package com.lankaspot.server.dto.common;

public record FeatureDefDTO(
        Long id,
        String name,
        String value,
        String unit,
        String icon
) {
    public FeatureDefDTO(Long id, String name, String value, String unit) {
        this(id, name, value, unit, null);
    }
}
