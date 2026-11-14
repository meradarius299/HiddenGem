package com.hiddengems.dto;

import java.util.List;

public record GemReviewsResponse(
        double averageRating,
        int ratingPercentage,
        long reviewCount,
        GemReviewResponse myReview,
        List<GemReviewResponse> reviews) {
}
