package com.lankaspot.server.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "advertisement_review_resubmit_queue")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdvertisementReviewResubmitQueue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "advertisement_id", referencedColumnName = "id", nullable = false)
    private Advertisement advertisement;


    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
