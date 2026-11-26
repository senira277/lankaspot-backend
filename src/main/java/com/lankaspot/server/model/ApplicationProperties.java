package com.lankaspot.server.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "m_application_properties")
@Data
@NoArgsConstructor
public class ApplicationProperties {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    private Long id;
    private String key;

    @Column(columnDefinition = "TEXT")
    private String value;
}
