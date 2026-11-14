package com.hiddengems.service.impl;

import com.hiddengems.dto.GemReviewRequest;
import com.hiddengems.dto.GemReviewResponse;
import com.hiddengems.dto.GemReviewsResponse;
import com.hiddengems.entity.Gem;
import com.hiddengems.entity.GemReview;
import com.hiddengems.entity.User;
import com.hiddengems.repository.GemRepository;
import com.hiddengems.repository.GemReviewRepository;
import com.hiddengems.repository.UserRepository;
import com.hiddengems.service.GemReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GemReviewServiceImpl implements GemReviewService {

    private final GemReviewRepository reviewRepository;
    private final GemRepository gemRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public GemReviewsResponse reviews(Long gemId, String username) {
        List<GemReview> reviews = reviewRepository.findByGemIdOrderByUpdatedAtDesc(gemId);
        if (!gemRepository.existsById(gemId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Gem not found");
        }
        long count = reviews.size();
        double average = count == 0 ? 0 : Math.round(reviews.stream().mapToInt(GemReview::getRating).average().orElse(0) * 10) / 10.0;
        int percentage = count == 0 ? 0 : (int) Math.round(average / 5 * 100);
        GemReviewResponse mine = username == null ? null : reviews.stream()
                .filter(r -> r.getUser().getUsername().equals(username))
                .findFirst().map(GemReviewResponse::from).orElse(null);
        return new GemReviewsResponse(average, percentage, count, mine,
                reviews.stream().map(GemReviewResponse::from).toList());
    }

    @Override
    @Transactional
    public GemReviewResponse save(Long gemId, GemReviewRequest request, String username) {
        Gem gem = gemRepository.findById(gemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Gem not found"));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown user"));
        GemReview review = reviewRepository.findByGemIdAndUserId(gemId, user.getId()).orElseGet(() -> {
            GemReview created = new GemReview();
            created.setGem(gem);
            created.setUser(user);
            return created;
        });
        review.setRating(request.rating());
        String comment = request.comment() == null ? null : request.comment().trim();
        review.setComment(comment == null || comment.isEmpty() ? null : comment);
        return GemReviewResponse.from(reviewRepository.save(review));
    }
}
