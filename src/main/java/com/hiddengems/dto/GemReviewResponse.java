package com.hiddengems.dto;

import com.hiddengems.entity.GemReview;

import java.time.LocalDateTime;

public record GemReviewResponse(
        Long id,
        Long userId,
        String username,
        int rating,
        String comment,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static GemReviewResponse from(GemReview review) {
        return new GemReviewResponse(review.getId(), review.getUser().getId(), review.getUser().getUsername(),
                review.getRating(), review.getComment(), review.getCreatedAt(), review.getUpdatedAt());
    }
}
