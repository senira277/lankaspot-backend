package com.lankaspot.server.model;

import com.lankaspot.server.enums.FeatureValueType;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "features")
@Data
public class Feature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "value", nullable = false, unique = true, length = 100)
    private Long value = 1L;

    @Enumerated(EnumType.STRING)
    @Column(name = "value_type", nullable = false, length = 10)
    private FeatureValueType valueType = FeatureValueType.NONE;

    @Column(name = "unit", length = 30)   // optional: "sq ft", "km", "months"
    private String unit;

    @Column(name = "icon", length = 50)
    private String icon;

    @Column(name = "slug", nullable = false, unique = true, length = 100)
    private String slug;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
}
