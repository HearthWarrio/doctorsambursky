package com.doctor.review.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "reviews")
@Data
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "author_name", nullable = false)
    private String authorName;

    @Column(name = "text", columnDefinition = "TEXT", nullable = false)
    private String text;

    @Column(name = "image_before_url")
    private String imageBeforeUrl;

    @Column(name = "image_after_url")
    private String imageAfterUrl;

    @Column(name = "approved", nullable = false)
    private Boolean approved = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
