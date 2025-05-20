package com.doctor.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReviewDto {
    private Long id;
    private String authorName;
    private String text;
    private String imageBeforeUrl;
    private String imageAfterUrl;
    private Boolean approved;
    private LocalDateTime createdAt;
}
