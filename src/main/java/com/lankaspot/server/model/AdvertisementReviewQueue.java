package com.lankaspot.server.model;


import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "advertisement_review_queue")
@Data
@NoArgsConstructor
public class AdvertisementReviewQueue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "advertisement_id", referencedColumnName = "id", nullable = false)
    private Advertisement advertisement;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

}
