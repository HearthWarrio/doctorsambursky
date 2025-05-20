package com.doctor.review.web;

import com.doctor.review.entity.Review;
import com.doctor.review.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
@Validated
public class ReviewController {
    private final ReviewService service;

    /** GET  /api/reviews — список одобренных. */
    @GetMapping
    public ResponseEntity<List<Review>> getReviews() {
        return ResponseEntity.ok(service.getApprovedReviews());
    }

    /** POST /api/reviews — добавить новый отзыв. */
    @PostMapping
    public ResponseEntity<Review> postReview(@Valid @RequestBody Review review) {
        return ResponseEntity.ok(service.saveReview(review));
    }

    /** PUT /api/reviews/{id}/approve — одобрить. */
    @PutMapping("/{id}/approve")
    public ResponseEntity<Void> approve(@PathVariable Long id) {
        service.approveReview(id);
        return ResponseEntity.ok().build();
    }

    /** DELETE /api/reviews/{id} — удалить. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteReview(id);
        return ResponseEntity.noContent().build();
    }
}
