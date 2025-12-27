package com.doctor.review.service;

import com.doctor.review.entity.Review;
import com.doctor.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewRepository repository;

    /** Вернуть все одобренные отзывы, отсортированные по дате создания. */
    public List<Review> getApprovedReviews() {
        return repository.findByApprovedTrueOrderByCreatedAtDesc();
    }

    /** Сохранить новый отзыв (по умолчанию: не одобрен). */
    public Review saveReview(Review review) {
        review.setCreatedAt(LocalDateTime.now());
        review.setApproved(false);
        return repository.save(review);
    }

    /** Одобрить отзыв по ID. */
    public void approveReview(Long id) {
        repository.findById(id).ifPresent(r -> {
            r.setApproved(true);
            repository.save(r);
        });
    }

    /** Удалить отзыв по ID. */
    public void deleteReview(Long id) {
        repository.deleteById(id);
    }
}
