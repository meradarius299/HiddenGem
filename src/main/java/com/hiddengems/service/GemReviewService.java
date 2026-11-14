package com.hiddengems.service;

import com.hiddengems.dto.GemReviewRequest;
import com.hiddengems.dto.GemReviewResponse;
import com.hiddengems.dto.GemReviewsResponse;

public interface GemReviewService {
    GemReviewsResponse reviews(Long gemId, String username);
    GemReviewResponse save(Long gemId, GemReviewRequest request, String username);
}
