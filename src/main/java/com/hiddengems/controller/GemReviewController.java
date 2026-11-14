package com.hiddengems.controller;

import com.hiddengems.dto.GemReviewRequest;
import com.hiddengems.dto.GemReviewResponse;
import com.hiddengems.dto.GemReviewsResponse;
import com.hiddengems.service.GemReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/gems/{gemId}/reviews")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
public class GemReviewController {

    private final GemReviewService reviewService;

    @GetMapping
    public GemReviewsResponse reviews(@PathVariable Long gemId, Authentication auth) {
        return reviewService.reviews(gemId, auth.getName());
    }

    @PostMapping
    public GemReviewResponse save(@PathVariable Long gemId,
                                  @Valid @RequestBody GemReviewRequest request,
                                  Authentication auth) {
        return reviewService.save(gemId, request, auth.getName());
    }
}
